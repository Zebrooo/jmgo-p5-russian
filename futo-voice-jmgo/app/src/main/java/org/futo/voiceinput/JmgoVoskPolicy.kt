package org.futo.voiceinput

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JmgoVoskPolicy {
    const val APPLICATION_ID = "org.futo.voiceinput.jmgo"
    const val MODEL_DIRECTORY = "vosk-model-small-ru-0.22"

    fun shouldUse(packageName: String, modelExists: Boolean): Boolean =
        packageName == APPLICATION_ID && modelExists

    fun shouldAcceptResult(result: String): Boolean = result.isNotBlank()

    fun toPcm16(samples: FloatArray): ShortArray = ShortArray(samples.size) { index ->
        when (val sample = samples[index].coerceIn(-1.0f, 1.0f)) {
            -1.0f -> Short.MIN_VALUE
            1.0f -> Short.MAX_VALUE
            else -> (sample * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun parseFinalResult(json: String): String = runCatching {
        Json.parseToJsonElement(json)
            .jsonObject["text"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            .orEmpty()
    }.getOrDefault("")
}
