package com.sleepysoong.logent.claudemem

import com.sleepysoong.logent.plugin.PluginHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

internal class MemoryEngineException(message: String) : IllegalStateException(message)

internal class RuntimeBridge {
    private val lock = Mutex()
    private var installedHost: PluginHost? = null
    private var runtime: String? = null

    suspend fun prepare(host: PluginHost) = lock.withLock {
        if (installedHost === host && runtime != null) return@withLock
        val bytes = host.readAsset(ASSET).getOrThrow()
        require(bytes.isNotEmpty()) { "패키지의 원본 엔진 번들이 비어 있어요. 서명된 .lpg를 다시 설치해 주세요." }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val archive = host.contentArchive(PREFIX).getOrThrow()
        val temporary = UUID.randomUUID().toString()
        val script = """
            export PATH=/data/data/com.termux/files/usr/bin:${'$'}PATH
            umask 077
            command -v node >/dev/null 2>&1 || { printf '%s' 'Node.js 22.13 이상과 내장 node:sqlite가 필요해요. Termux에서 직접 준비한 뒤 다시 실행해 주세요.'; exit 1; }
            node --disable-warning=ExperimentalWarning --input-type=module -e 'import {DatabaseSync} from "node:sqlite"; if(Number(process.versions.node.split(".")[0])<22) process.exit(1); const db=new DatabaseSync(":memory:"); db.exec("CREATE VIRTUAL TABLE prerequisite USING fts5(text)"); db.close();' || { printf '%s' 'Node.js 22.13 이상과 내장 SQLite FTS5가 필요해요. 자동 설치하지 않아요.'; exit 1; }
            command -v unzip >/dev/null 2>&1 && command -v sha256sum >/dev/null 2>&1 || { printf '%s' 'Termux에 unzip과 sha256sum을 직접 준비해 주세요.'; exit 1; }
            ROOT="${'$'}HOME/.logent/plugins/claude-mem/runtime/$digest"
            mkdir -p "${'$'}ROOT" || exit 1
            TARGET="${'$'}ROOT/claude-mem.mjs"
            ACTUAL=''
            if [ -f "${'$'}TARGET" ]; then ACTUAL=${'$'}(sha256sum "${'$'}TARGET") || exit 1; fi
            if [ "${'$'}{ACTUAL%% *}" != '$digest' ]; then
              TEMP="${'$'}ROOT/.$temporary.part"
              trap 'rm -f "${'$'}TEMP"' EXIT HUP INT TERM
              unzip -p ${quote(archive)} '$ASSET_ENTRY' > "${'$'}TEMP" || exit 1
              ACTUAL=${'$'}(sha256sum "${'$'}TEMP") || exit 1
              [ "${'$'}{ACTUAL%% *}" = '$digest' ] || { printf '%s' '검증된 엔진 자산의 해시가 일치하지 않아요.'; exit 1; }
              chmod 600 "${'$'}TEMP" && mv -f "${'$'}TEMP" "${'$'}TARGET" || exit 1
            fi
            printf '\nLOGENT_RUNTIME=%s\n' "${'$'}TARGET"
        """.trimIndent()
        val result = host.shell(script, 60).getOrThrow()
        check(result.success && !result.truncated) {
            "원본 엔진을 준비하지 못했어요. Node.js 22.13 이상(node:sqlite·FTS5), unzip, sha256sum이 필요해요. 자동 설치나 재시도는 하지 않아요."
        }
        val path = result.output.lineSequence().firstOrNull { it.startsWith("LOGENT_RUNTIME=/") }
            ?.removePrefix("LOGENT_RUNTIME=")
        require(path != null && path.none { it == '\u0000' || it == '\r' }) { "준비된 엔진 경로를 받지 못했어요." }
        installedHost = host
        runtime = path
    }

    suspend fun request(host: PluginHost, request: JsonObject): JsonObject {
        prepare(host)
        val path = lock.withLock { requireNotNull(runtime) }
        val input = request.toString().toByteArray(Charsets.UTF_8)
        require(input.size <= 1_048_576) { "기억 요청은 1MiB 이하여야 해요." }
        val encoded = Base64.getEncoder().encodeToString(input)
        val command = """
            export PATH=/data/data/com.termux/files/usr/bin:${'$'}PATH
            umask 077
            printf '%s' '$encoded' | base64 -d | node --disable-warning=ExperimentalWarning ${quote(path)} --data-dir "${'$'}HOME/.logent/plugins/claude-mem/data"
        """.trimIndent()
        val result = host.shell(command, 60).getOrThrow()
        check(!result.truncated) { "엔진 응답이 잘렸어요. limit이나 요청 크기를 줄여서 직접 다시 실행해 주세요." }
        val output = result.output
        val start = output.indexOf('{')
        val end = output.lastIndexOf('}')
        require(start >= 0 && end >= start) {
            "원본 엔진 응답을 받지 못했어요. Node.js 22.13 이상(node:sqlite·FTS5)과 패키지 자산을 확인해 주세요."
        }
        val response = try { Json.parseToJsonElement(output.substring(start, end + 1)).jsonObject } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { throw IllegalStateException("원본 엔진 응답을 읽지 못했어요. Node.js·패키지 버전을 확인해 주세요.") }
        if (!result.success || response["ok"]?.jsonPrimitive?.booleanOrNull != true) {
            val code = (response["error"] as? JsonObject)?.get("code")?.jsonPrimitive?.contentOrNull
                ?.takeIf { Regex("[A-Z_a-z0-9-]{1,64}").matches(it) } ?: "ENGINE_ERROR"
            throw MemoryEngineException("기억 요청을 처리하지 못했어요($code). 입력·프로젝트 경로·Node.js 준비 상태를 확인해 주세요. 자동 재시도하지 않았어요.")
        }
        return response
    }

    companion object {
        const val PREFIX = "plugins/claude-mem/runtime"
        const val ASSET = "$PREFIX/claude-mem.mjs"
        private const val ASSET_ENTRY = "assets/$ASSET"
        fun quote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"
    }
}
