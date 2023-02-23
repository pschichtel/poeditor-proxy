package tel.schich.poeditorproxy

import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.CacheControl.Visibility.Private
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.Parameters
import io.ktor.http.content.CachingOptions
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KLogger
import mu.KotlinLogging

const val API_BASE_URL = "https://api.poeditor.com/v2"
const val API_TOKEN_ENV = "POEDITOR_API_TOKEN"
const val PROJECT_ID_ENV = "POEDITOR_PROJECT_ID"
const val FORCE_CONTENT_TYPE_ENV = "FORCED_CONTENT_TYPE"
const val NO_CACHE_ENV = "NO_CACHE"

const val CONNECT_TIMEOUT_MILLIS = 2000L
const val REQUEST_TIMEOUT_MILLIS = 10000L
const val MAX_CACHE_SIZE = 100L
const val BIND_PORT = 8080

@Serializable
data class PoEditorResponse(val response: ResponseStatus, val result: JsonElement? = null)

@Serializable
data class ResponseStatus(val status: String, val code: String, val message: String)

@Serializable
data class ExportResult(val url: String)

fun main() {
    embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment {
            rootPath = System.getenv("ROOT_PATH")?.trim() ?: ""
            module {
                setup()
            }
            connector {
                port = BIND_PORT
                host = "0.0.0.0"
            }
        },
    ).start(wait = true)
}

private fun Application.setup() {
    val apiToken = System.getenv(API_TOKEN_ENV) ?: error("No value given for $API_TOKEN_ENV!")
    val projectId = System.getenv(PROJECT_ID_ENV) ?: error("No value given for $PROJECT_ID_ENV!")
    val forcedContentType = System.getenv(FORCE_CONTENT_TYPE_ENV)?.let(ContentType::parse)
    val disableCaching = System.getenv(NO_CACHE_ENV)?.let(String::toBoolean) ?: false
    val logger = KotlinLogging.logger("poeditor-proxy")

    val cache = Cache.Builder()
        .maximumCacheSize(MAX_CACHE_SIZE)
        .build<String, ExportedFile>()

    val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }

    install(CORS) {
        allowNonSimpleContentTypes = true
        anyHost()
    }
    install(CachingHeaders) {
        options { call, _ ->
            if (call.response.status() == OK) {
                CachingOptions(NoStore(Private))
            } else {
                null
            }
        }
    }
    routing {
        get("/export/{type}/{file}") {
            val type = call.parameters["type"]!!

            val fileName = call.parameters["file"]!!
            val regex = """([^.]+).*""".toRegex()
            val match = regex.matchEntire(fileName)
            if (match == null) {
                call.respondText(status = BadRequest) {
                    "File needs to be <language-code>.<ext> or just <language-code>!"
                }
                return@get
            }
            val (language) = match.destructured

            exportAndReturnLanguage(
                client,
                cache,
                logger,
                apiToken,
                projectId,
                language,
                type,
                forcedContentType,
                disableCaching,
            )
        }
    }
}

@Suppress("LongParameterList")
private suspend fun PipelineContext<Unit, ApplicationCall>.exportAndReturnLanguage(
    client: HttpClient,
    cache: Cache<String, ExportedFile>,
    logger: KLogger,
    apiToken: String,
    projectId: String,
    language: String,
    type: String,
    forcedContentType: ContentType?,
    disableCaching: Boolean,
) {
    val cacheKey = "$language.$type"
    val exportResponse: HttpResponse = client.submitForm(
        url = "$API_BASE_URL/projects/export",
        formParameters = Parameters.build {
            append("api_token", apiToken)
            append("id", projectId)
            append("language", language.lowercase())
            append("type", type)
            // append("filters", "")
            append("order", "terms")
            // append("tags", "")
            // append("options", "")
        },
        encodeInQuery = false,
    )

    val text = exportResponse.bodyAsText()
    val result = try {
        val response = Json.decodeFromString<PoEditorResponse>(text)
        val result = response.result
        if (result == null) {
            respondFromCacheOrError(
                cache,
                cacheKey,
                "${response.response.code} - ${response.response.message}",
                exportResponse,
            )
            return
        }
        result
    } catch (e: SerializationException) {
        logger.error(e) { "Failed to process response!" }
        respondFromCacheOrError(
            cache,
            cacheKey,
            "Deserialization failed: ${e.message}",
            exportResponse,
        )
        return
    }

    val (url) = Json.decodeFromJsonElement<ExportResult>(result)

    val downloadResponse: HttpResponse = client.get(url)
    val contentType = forcedContentType ?: downloadResponse.contentType()
    val data = downloadResponse.readBytes()
    if (!disableCaching) {
        cache.put(cacheKey, ExportedFile(contentType, downloadResponse.status, data))
    }
    call.respondBytes(data, contentType, downloadResponse.status)
}

private suspend fun PipelineContext<Unit, ApplicationCall>.respondFromCacheOrError(
    cache: Cache<String, ExportedFile>,
    cacheKey: String,
    reason: String,
    response: HttpResponse,
) {
    val cached = cache.get(cacheKey)
    if (cached != null) {
        call.response.header("X-POEditor-Reason", reason)
        call.respondBytes(cached.data, cached.contentType, cached.status)
    } else {
        call.respondText(response.contentType(), response.status) { response.bodyAsText() }
    }
}

class ExportedFile(val contentType: ContentType?, val status: HttpStatusCode, val data: ByteArray)
