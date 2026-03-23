package tel.schich.poeditorproxy

import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import com.sksamuel.hoplite.watch.ReloadableConfig
import com.sksamuel.hoplite.watch.watchers.FileWatcher
import io.ktor.http.ContentType
import java.nio.file.Path
import kotlin.reflect.KType

object ContentTypeDecoder : Decoder<ContentType> {
    override fun decode(
        node: Node,
        type: KType,
        context: DecoderContext
    ): ConfigResult<ContentType> {
        return when (node) {
            is StringNode -> ContentType.parse(node.value).valid()
            else -> ConfigFailure.DecodeError(node, type).invalid()
        }
    }

    override fun supports(type: KType) =
        type.classifier == ContentType::class
}

data class Project(
    val id: String,
    val tokens: List<String>,
    val forceContentType: ContentType? = null,
    val caching: Boolean = true,
)

data class Config(val projects: Map<String, Project> = mapOf()) {
    companion object {
        fun load(file: Path): ReloadableConfig<Config> {
            val fileWatcher = FileWatcher(file.parent.toString())
            val configLoader = ConfigLoaderBuilder.default()
                .addSource(PropertySource.file(file.toFile(), optional = true, allowEmpty = true))
                .addSource(PropertySource.resource("/default-config.yml"))
                .addDecoder(ContentTypeDecoder)
                .build()

            return ReloadableConfig(configLoader, Config::class)
                .addWatcher(fileWatcher)
        }
    }
}
