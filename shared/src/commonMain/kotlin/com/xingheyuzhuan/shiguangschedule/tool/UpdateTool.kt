package com.xingheyuzhuan.shiguangschedule.tool

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

enum class UpdatePlatform(val title: String) {
    GITHUB("GitHub 官方")
}

sealed class UpdateStatus {
    data class Found(
        val versionName: String,
        val changelog: String,
        val targetUrl: String,
        val isDirectDownload: Boolean
    ) : UpdateStatus()

    data class Latest(val currentVersion: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
    data object NotSupported : UpdateStatus()
    data object Checking : UpdateStatus()
    data object Idle : UpdateStatus()
}

@Serializable
data class ApiReleaseResponse(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("assets") val assets: List<ApiAsset> = emptyList()
)

@Serializable
data class ApiAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = ""
)

expect object PlatformUpdateStrategy {
    val isUpdateSupported: Boolean
    fun parseTargetUrl(response: ApiReleaseResponse): String?
    fun openUrl(url: String)
}

/**
 * 添加 @Single 注解，SharedModule 的 @ComponentScan 会自动发现并注册为单例
 */
@Single
class UpdateChecker(
    private val httpClient: HttpClient = defaultHttpClient
) {
    companion object {
        private const val GITHUB_REPO = "zzemy/shiguangschedule"
        private const val USER_AGENT = "ShiguangNjupt-Android"

        private val GITHUB_API_URL =
            "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

        /** 直连与代理镜像按顺序尝试，直连失败时自动切换。 */
        private val GITHUB_ENDPOINTS = listOf(
            GitHubEndpoint(apiUrl = GITHUB_API_URL, proxyPrefix = null),
            GitHubEndpoint(
                apiUrl = "https://mirror.ghproxy.com/$GITHUB_API_URL",
                proxyPrefix = "https://mirror.ghproxy.com/"
            ),
            GitHubEndpoint(
                apiUrl = "https://ghfast.top/$GITHUB_API_URL",
                proxyPrefix = "https://ghfast.top/"
            )
        )

        val defaultHttpClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 15_000
                }
            }
        }
    }

    private data class GitHubEndpoint(
        val apiUrl: String,
        val proxyPrefix: String?
    ) {
        /** 镜像响应中的下载地址仍指向 GitHub，下载时同样走镜像。 */
        fun proxy(url: String): String {
            if (proxyPrefix == null) return url
            return if (url.startsWith("https://github.com/") ||
                url.startsWith("https://api.github.com/")
            ) {
                "$proxyPrefix$url"
            } else {
                url
            }
        }
    }

    suspend fun checkUpdate(
        platform: UpdatePlatform,
        currentVersionName: String
    ): UpdateStatus = withContext(Dispatchers.IO) {
        if (!PlatformUpdateStrategy.isUpdateSupported) {
            return@withContext UpdateStatus.NotSupported
        }

        val endpoints = when (platform) {
            UpdatePlatform.GITHUB -> GITHUB_ENDPOINTS
        }

        var lastFailure: Exception? = null
        for (endpoint in endpoints) {
            try {
                val response = fetchLatestRelease(endpoint.apiUrl)
                if (response.tagName.isBlank()) {
                    lastFailure = IllegalStateException("更新响应缺少版本号")
                    continue
                }

                val latestVersion = response.tagName.removePrefix("v").removePrefix("V").trim()
                val currentVersion = currentVersionName.removePrefix("v").removePrefix("V").trim()

                if (!isNewerVersion(latestVersion, currentVersion)) {
                    return@withContext UpdateStatus.Latest(currentVersionName)
                }

                val directDownloadUrl = PlatformUpdateStrategy.parseTargetUrl(response)
                val releaseTagUrl =
                    "https://github.com/$GITHUB_REPO/releases/tag/${response.tagName}"

                val (targetUrl, isDirectDownload) = if (!directDownloadUrl.isNullOrEmpty()) {
                    Pair(endpoint.proxy(directDownloadUrl), true)
                } else {
                    Pair(endpoint.proxy(releaseTagUrl), false)
                }

                return@withContext UpdateStatus.Found(
                    versionName = response.tagName,
                    changelog = response.body,
                    targetUrl = targetUrl,
                    isDirectDownload = isDirectDownload
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
            }
        }

        UpdateStatus.Error(friendlyFailureMessage(lastFailure))
    }

    private suspend fun fetchLatestRelease(apiUrl: String): ApiReleaseResponse {
        val response = httpClient.get(apiUrl) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/vnd.github+json")
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException("HTTP ${response.status.value}")
        }
        return response.body()
    }

    private fun friendlyFailureMessage(error: Exception?): String {
        val detail = error?.message.orEmpty()
        return when {
            error is io.ktor.client.plugins.HttpRequestTimeoutException ||
                detail.contains("timeout", ignoreCase = true) ||
                detail.contains("timed out", ignoreCase = true) ->
                "网络连接超时，请检查网络后重试"

            detail.contains("403") || detail.contains("401") ->
                "GitHub 访问受限，请稍后重试"

            else ->
                "无法连接更新服务器，已尝试 GitHub 直连与镜像，请检查网络后重试"
        }
    }

    /**
     * 比较版本号：判断 latest 是否大于 current
     */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split('.', '-').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.', '-').mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun launchUpdate(targetUrl: String) {
        PlatformUpdateStrategy.openUrl(targetUrl)
    }
}