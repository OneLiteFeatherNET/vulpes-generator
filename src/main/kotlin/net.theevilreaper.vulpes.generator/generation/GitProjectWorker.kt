package net.theevilreaper.vulpes.generator.generation

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File


/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

class GitProjectWorker(
    private val basePath: File,
    private val remoteUrl: String,
    private val reference: String,
) {

    private val logger: Logger = LoggerFactory.getLogger(GitProjectWorker::class.java)

    fun cloneAndCheckout() {
        try {
            val rawGit =
                Git.cloneRepository().setURI(remoteUrl).setDirectory(basePath).setCloneAllBranches(true)
            val git = rawGit.call()
            logger.info("Git reference is $reference")
            val objectId = git.repository.resolve(reference)
            if (objectId != null) {
                logger.info("Name from the git object is ${objectId.name}")
                git.checkout().setName(objectId.name()).call()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: GitAPIException) {
            e.printStackTrace()
        }
    }
}