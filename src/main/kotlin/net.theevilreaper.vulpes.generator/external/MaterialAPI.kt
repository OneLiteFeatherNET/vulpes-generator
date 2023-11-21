package net.theevilreaper.vulpes.generator.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import net.theevilreaper.vulpes.generator.generation.GeneratorRegistry
import net.theevilreaper.vulpes.generator.generation.dart.MaterialGenerator
import net.theevilreaper.vulpes.model.MaterialWrapper
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import kotlin.io.path.createTempDirectory

@RestController
@Service
class MaterialAPI(
    val registry: GeneratorRegistry,
) {

    @Value(value = "https://github.com/Articdive/ArticData.git")
    private lateinit var remoteUrl: String

    @Value(value = "19")
    private lateinit var supportedMainVersion: String

    private var cache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .build<String, Map<String, MaterialWrapper>>()

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val typeReference: TypeReference<Map<String, MaterialWrapper>> =
        object : TypeReference<Map<String, MaterialWrapper>>() {}

    @RequestMapping("material/branches", method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getVersions(): ResponseEntity<List<String>> {
        val refs = Git.lsRemoteRepository().setHeads(true).setRemote(remoteUrl).call().map { it.name }.toList()
        val branches = refs.map { it.substringAfter("refs/heads/") }.filter { it.contains(supportedMainVersion) }.sorted()
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(branches)
    }

    @RequestMapping(
        "material/generate/{version}",
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun generate(@PathVariable("version") version: String): ResponseEntity<Any> {
        val data: Map<String, MaterialWrapper> = cache.getIfPresent(version) ?: fetchMaterialByVersion(version)
        val generator: MaterialGenerator = registry.getGenerator("MaterialGenerator") as MaterialGenerator
        generator.updateGenerationData(data.values.toList())
        val temp = createTempDirectory("Dart")
        generator.generate(temp)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=materials.dart")
            .body(FileSystemResource(temp.resolve("materials.dart").toFile()))
    }

    /**
     * Generates the material data by a given version.
     * @param version the version to fetch the material data
     * @return the mapped material data as map with string as key and [MaterialWrapper] as value
     */
    fun fetchMaterialByVersion(version: String): Map<String, MaterialWrapper> {
        val tempDir = createTempDirectory("${version}-Temp")
        Git.cloneRepository().setBranch("refs/heads/$version").setURI(remoteUrl).setDirectory(tempDir.toFile()).call()

        val path = tempDir.resolve("${version.replace(".", "_")}_items.json")
        val fileContent: Map<String, MaterialWrapper> = objectMapper.readValue(path.toFile(), typeReference).toSortedMap()
        cache.put(version, fileContent)
        return fileContent
    }
}