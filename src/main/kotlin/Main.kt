import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

const val API_BASE_URL = "https://api.poeditor.com/v2"
const val API_TOKEN_ENV = "POEDITOR_API_TOKEN"
const val PROJECT_ID_ENV = "POEDITOR_PROJECT_ID"

@Serializable
data class PoEditorResponse(val response: ResponseStatus, val result: JsonElement? = null)
@Serializable
data class ResponseStatus(val status: String, val code: String, val message: String)
@Serializable
data class ExportResult(val url: String)

fun main(args: Array<String>) {

    val apiToken = System.getenv(API_TOKEN_ENV) ?: error("No value given for $API_TOKEN_ENV!")
    val projectId = System.getenv(PROJECT_ID_ENV) ?: error("No value given for $PROJECT_ID_ENV!")

    val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 2000
            requestTimeoutMillis = 10000
        }
    }

    embeddedServer(Netty, port = 8080) {

        routing {
            get("/export/{type}/{file}") {
                val type = call.parameters["type"]!!

                val file = call.parameters["file"]!!
                val regex = """([^.]+).*""".toRegex()
                val match = regex.matchEntire(file)
                if (match == null) {
                    call.respondText(status = HttpStatusCode.BadRequest) { "File needs to be <language-code>.<ext> or just <language-code>!" }
                    return@get
                }
                val (language) = match.destructured

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

                val text = exportResponse.readText()
                val response = Json.decodeFromString<PoEditorResponse>(text)
                val result = response.result
                if (result == null) {
                    call.respondText(exportResponse.contentType(), exportResponse.status) { text }
                    return@get
                }

                val (url) = Json.decodeFromJsonElement<ExportResult>(result)

                val downloadResponse: HttpResponse = client.get(url)
                call.respondBytes(downloadResponse.readBytes(), downloadResponse.contentType(), downloadResponse.status)
            }
        }

    }.start(wait = true)
}