package com.lfwqsp2641.scunet_login.data.utils

import com.lfwqsp2641.scunet_login.data.dto.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object ConfigSerializer : androidx.datastore.core.Serializer<Config> {
    override val defaultValue: Config = Config(accounts = emptyList())

    override suspend fun readFrom(input: InputStream): Config = withContext(Dispatchers.IO) {
        try {
            Json.decodeFromString(
                deserializer = Config.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: Config, output: OutputStream) = withContext(Dispatchers.IO) {
        output.write(Json.encodeToString(Config.serializer(), t).encodeToByteArray())
    }
}