package net.theevilreaper.vulpes.generator.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import net.theevilreaper.vulpes.generator.external.MaterialAPI
import net.theevilreaper.vulpes.generator.spec.MinecraftDataSpec
import net.theevilreaper.vulpes.model.MaterialWrapper
import org.eclipse.jgit.api.Git
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createTempDirectory

@Component
class MinecraftDataCache(
    val materialAPI: MaterialAPI
) {
    @Value(value = "https://github.com/Articdive/ArticData.git")
    private lateinit var remoteUrl: String

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val typeReference: TypeReference<Map<String, MaterialWrapper>> = object : TypeReference<Map<String, MaterialWrapper>>() {}

    private val cacheTimeOut: Long = 10L

    private val dataCache: AsyncCache<String, MinecraftDataSpec> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(cacheTimeOut))
        .recordStats()
        .buildAsync()

    fun getDataFromCache(version: String): CompletableFuture<MinecraftDataSpec> {
        return dataCache.get(version) { _, executor ->
            CompletableFuture.supplyAsync( { fetchDataByVersion(version)}, executor)
        }
    }

    /**
     * Generates the material data by a given version.
     * @param version the version to fetch the material data
     * @return the mapped material data as map with string as key and [MaterialWrapper] as value
     */
    fun fetchDataByVersion(version: String): MinecraftDataSpec {
        val tempDir = createTempDirectory("${version}-Temp")
        Git.cloneRepository().setBranch("refs/heads/$version").setURI(remoteUrl).setDirectory(tempDir.toFile()).call()

        val path = tempDir.resolve("${version.replace(".", "_")}_items.json")
        val fileContent: Map<String, MaterialWrapper> = objectMapper.readValue(path.toFile(), typeReference).toSortedMap()

        return MinecraftDataSpec(version = version, materials = fileContent.values.toList())
    }
}