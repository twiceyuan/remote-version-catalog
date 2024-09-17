package io.github.twiceyuan.rvc

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.util.GradleVersion
import java.io.File
import java.net.URL
import javax.inject.Inject
import kotlin.io.path.Path

/**
 * Load config file of version catalog from remote server.
 */
@Suppress("unused")
class RemoteVersionCatalogPlugin @Inject constructor(
    private val objects: ObjectFactory
) : Plugin<Settings> {

    private val logger = Logging.getLogger("remote-version-catalog")

    companion object {
        /**
         * Property name of remote toml file url.
         *
         * eg: https://example.com/libs.versions.toml
         */
        const val PROP_REMOTE_URL = "remote.version.catalog.url"

        /**
         * Property name of Remote VersionCatalog config name
         *
         * eg: common
         */
        const val PROP_REMOTE_CATALOG_NAME = "remote.version.catalog.name"

        /**
         * Local storage path for VersionCatalog toml file
         *
         * default value: .gradle/
         */
        const val PROP_REMOTE_CATALOG_PATH = "remote.version.catalog.path"

        /**
         * Expire time of local toml file cache.
         *
         * Default value is one day. (86400000, unit is millis)
         */
        const val PROP_EXPIRE_MILLIS = "remote.version.catalog.expire"

        /**
         * The file suffix of toml file
         */
        const val FILE_NAME_SUFFIX = ".versions.toml"

        /**
         * Default local cache expire time (millis)
         */
        const val DEFAULT_CACHE_EXPIRE_MILLIS = 86400000L

        /**
         * The file suffix that stores the timestamp of the last update of the toml file
         *
         * eg: toml file name -> xxx.versions.toml
         *     update at flag file name -> xxx.version.toml.update_at.txt
         */
        const val LAST_UPDATE_AT_SUFFIX = ".update_at.txt"
    }

    override fun apply(settings: Settings) {
        checkGradleVersion()

        // read the properties
        val properties = settings.extensions.extraProperties
        val url = properties[PROP_REMOTE_URL]?.toString()
        val name = properties[PROP_REMOTE_CATALOG_NAME]?.toString()
        val expireMillis =
            properties.opt(PROP_EXPIRE_MILLIS)?.toString()?.toLong() ?: DEFAULT_CACHE_EXPIRE_MILLIS

        val dotGradleDir = Path("${settings.rootDir}/.gradle").toFile()
        val fileName = "${name}${FILE_NAME_SUFFIX}"
        val storagePath = properties.opt(PROP_REMOTE_CATALOG_PATH)?.toString()
        val storagePathFile = storagePath?.let { Path("${settings.rootDir}/$it").toFile() }
        val remoteFile = if (storagePathFile != null) {
            if (storagePathFile.isDirectory) {
                File(storagePathFile, "${name}${FILE_NAME_SUFFIX}")
            } else {
                errorOccur("Path is not exist, please check property: $PROP_REMOTE_CATALOG_PATH=${storagePathFile.path}")
            }
        } else {
            File(dotGradleDir, "${name}${FILE_NAME_SUFFIX}")
        }
        val lastUpdateAtFile = File(dotGradleDir, "$fileName$LAST_UPDATE_AT_SUFFIX")

        if (url.isNullOrBlank()) {
            errorOccur("Please define the property (${PROP_REMOTE_URL})")
        }

        if (name.isNullOrBlank()) {
            errorOccur("Please define the property（${PROP_REMOTE_CATALOG_NAME}）")
        }

        // If cache file is expired or not exist, download the file.
        val isExpired =
            System.currentTimeMillis() - getLastUpdateAt(lastUpdateAtFile) > expireMillis
        val isFileExist = remoteFile.exists() && remoteFile.length() > 0
        if (isExpired || isFileExist.not()) {
            logger.info("Download VersionCatalog file... $url")
            downloadRemoteVersionCatalogFile(url, remoteFile)
            writeLastUpdateAt(lastUpdateAtFile, System.currentTimeMillis())
            logger.info("Download successfully.")
        } else {
            logger.info("Version catalog file cache is valid, skip download (url: $url)")
        }

        if (name != "libs") {
            settings.gradle.settingsEvaluated {
                @Suppress("UnstableApiUsage")
                settings.dependencyResolutionManagement.versionCatalogs.apply {
                    register(name) {
                        it.from(objects.fileCollection().from(remoteFile))
                    }
                }
            }
        }

        settings.gradle.afterProject { project ->
            // Register the task for force download configuration file. (Skip the expiry time config)
            project.tasks.register("downloadRemoteVersionCatalog") { task ->
                task.group = "remote-version-catalog"
                task.doLast {
                    downloadRemoteVersionCatalogFile(url, remoteFile)
                    writeLastUpdateAt(lastUpdateAtFile, System.currentTimeMillis())
                }
            }

            project.tasks.register("cleanRemoteVersionCatalog") { task ->
                task.group = "remote-version-catalog"
                task.doLast {
                    remoteFile.delete()
                    lastUpdateAtFile.delete()
                    logger.info("The local VersionCatalog cache deleted. (${remoteFile.path})")
                }
            }
        }
    }

    /**
     * Write last update time of toml file
     */
    private fun writeLastUpdateAt(flagFile: File, currentTimeMillis: Long) {
        flagFile.writeText(currentTimeMillis.toString())
    }

    /**
     * Read last update time of toml file
     */
    private fun getLastUpdateAt(flagFile: File): Long {
        return runCatching { flagFile.readText().trim().toLong() }.getOrNull() ?: 0L
    }

    /**
     * 下载远程配置文件
     */
    private fun downloadRemoteVersionCatalogFile(url: String, remoteFile: File) {
        URL(url).openStream().use {
            val remoteContent = it.reader().readText()
            if (remoteContent.isNotBlank()) {
                remoteFile.writeText(remoteContent)
            } else {
                errorOccur("Content is empty: $url")
            }
        }
    }

    private fun checkGradleVersion() {
        if (GradleVersion.current() < GradleVersion.version("7.2")) {
            errorOccur("Gradle version must be greater than 7.2, otherwise VersionCatalog feature cannot be used.")
        }
    }

    private fun errorOccur(message: String): Nothing {
        logger.error(message)
        error(message)
    }

    private fun ExtraPropertiesExtension.opt(name: String) = if (has(name)) get(name) else null
}
