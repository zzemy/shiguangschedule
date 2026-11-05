package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import shiguangschedule.shared.generated.resources.Res


/**
 * 贡献者数据仓库
 */
@Single
class ContributionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 读取贡献者 JSON 数据并反序列化。
     *
     * 资源缺失或解析失败时返回内置的默认贡献者列表，保证贡献者页面始终可用。
     */
    suspend fun getContributions(): ContributionList = withContext(Dispatchers.IO) {
        try {
            // 直接跨平台读取打包进去的静态资源文件
            val bytes = Res.readBytes("files/contributors_data/contributors.json")
            val jsonString = bytes.decodeToString()
            json.decodeFromString<ContributionList>(jsonString)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            defaultContributions()
        }
    }

    private fun defaultContributions(): ContributionList = ContributionList(
        appDev = listOf(
            ContributionList.Contributor(
                name = "zzemy",
                url = "https://github.com/zzemy",
                avatar = "avatars/zzemy.webp"
            ),
            ContributionList.Contributor(
                name = "XingHeYuZhuan",
                url = "https://github.com/XingHeYuZhuan",
                avatar = "avatars/xingheyuzhuan.webp"
            ),
            ContributionList.Contributor(
                name = "kemi-20",
                url = "https://github.com/kemi-20",
                avatar = "avatars/kemi20.webp"
            )
        ),
        jiaowuadapter = listOf(
            ContributionList.Contributor(
                name = "kemi-20",
                url = "https://github.com/kemi-20",
                avatar = "avatars/kemi20.webp"
            ),
            ContributionList.Contributor(
                name = "zzemy",
                url = "https://github.com/zzemy",
                avatar = "avatars/zzemy.webp"
            ),
            ContributionList.Contributor(
                name = "XingHeYuZhuan",
                url = "https://github.com/XingHeYuZhuan",
                avatar = "avatars/xingheyuzhuan.webp"
            )
        )
    )
}