package com.xingheyuzhuan.shiguangschedule.data.api.exam

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/**
 * 南邮 StuDev 班级在线考试数据接口异常。
 */
class ExamSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 南邮 StuDev 班级在线考试同步客户端。
 *
 * 数据链路：
 * 1. `/pwa-release.json` 获取数据版本与服务端学期；
 * 2. `{base}/exams/{exams}/index.json` 获取班级分组列表；
 * 3. 按班级代码匹配分组后，读取 `classes/{entry}.json` 中的考试明细。
 */
@Single
class NjuptExamSyncClient(
    private val httpClient: HttpClient = defaultHttpClient
) {

    companion object {
        private const val HOST = "https://studev.hicancan.top"

        val defaultHttpClient by lazy {
            HttpClient {
                expectSuccess = true
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
            }
        }
    }

    /**
     * 按班级代码查询考试安排。
     *
     * @param classCode 班级代码（如 B241505），匹配时忽略大小写与空白字符。
     * @throws ExamSyncException 参数为空、网络异常、数据异常或未找到班级时抛出。
     */
    suspend fun fetchExams(classCode: String): List<ExamImportItem> = withContext(Dispatchers.IO) {
        val normalizedCode = normalizeCode(classCode)
        if (normalizedCode.isEmpty()) throw ExamSyncException("请输入班级代码")

        try {
            val release = httpClient.get("$HOST/pwa-release.json").body<ReleaseManifest>()
            val base = release.base.trim().trimEnd('/')
            val term = release.exams.trim()
            if (base.isEmpty() || term.isEmpty()) {
                throw ExamSyncException("考试服务数据异常，请稍后重试")
            }

            val index = httpClient
                .get("$HOST$base/exams/$term/index.json")
                .body<ExamsIndex>()

            val classEntry = index.classes.firstOrNull { entry ->
                entry.split(',').any { token -> matchesClassCode(token, normalizedCode) }
            } ?: throw ExamSyncException("未找到班级 $normalizedCode 的考试安排，请检查班级代码")

            val detail = httpClient
                .get("$HOST$base/exams/$term/classes/$classEntry.json")
                .body<ClassExamsResponse>()

            detail.exams
                .mapNotNull { it.toImportItem() }
                .sortedWith(compareBy<ExamImportItem> { it.date }.thenBy { it.startTime ?: "99:99" })
        } catch (e: CancellationException) {
            throw e
        } catch (e: ExamSyncException) {
            throw e
        } catch (e: Exception) {
            throw ExamSyncException("考试服务连接失败，请检查网络后重试", e)
        }
    }

    private fun normalizeCode(value: String): String =
        value.filterNot { it.isWhitespace() }.uppercase()

    /**
     * 支持精确匹配，以及带方向/校区后缀的分组标记，如 `B230601(GF)`。
     */
    private fun matchesClassCode(token: String, normalizedCode: String): Boolean {
        val normalizedToken = normalizeCode(token)
        return normalizedToken == normalizedCode || normalizedToken.startsWith("$normalizedCode(")
    }

    private fun RemoteExam.toImportItem(): ExamImportItem? {
        val courseName = course?.name?.trim().orEmpty()
        val dateValue = date.trim()
        if (courseName.isEmpty() || dateValue.isEmpty()) return null

        val roomName = location?.trim()?.takeIf { it.isNotEmpty() }
            ?: room?.trim()?.substringAfterLast('/')?.trim()?.takeIf { it.isNotEmpty() }

        return ExamImportItem(
            courseName = courseName,
            date = dateValue,
            startTime = start?.trim()?.takeIf { it.isNotEmpty() },
            endTime = end?.trim()?.takeIf { it.isNotEmpty() },
            location = roomName,
            teacher = formatTeacher(teacher),
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            campus = campus?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * 服务端教师字段形如 `工号/姓名,工号/姓名`，展示时仅保留姓名。
     */
    private fun formatTeacher(raw: String?): String? {
        val names = raw
            ?.split(',')
            ?.map { it.substringAfter('/', it).trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return names.takeIf { it.isNotEmpty() }?.joinToString(" / ")
    }
}

@Serializable
private data class ReleaseManifest(
    val base: String = "",
    val exams: String = ""
)

@Serializable
private data class ExamsIndex(
    val classes: List<String> = emptyList()
)

@Serializable
private data class ClassExamsResponse(
    val exams: List<RemoteExam> = emptyList()
)

@Serializable
private data class RemoteExam(
    val course: RemoteCourse? = null,
    val teacher: String? = null,
    val campus: String? = null,
    val location: String? = null,
    val room: String? = null,
    val date: String = "",
    val start: String? = null,
    val end: String? = null,
    val note: String? = null
)

@Serializable
private data class RemoteCourse(
    val name: String = ""
)