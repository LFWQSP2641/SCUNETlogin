package com.lfwqsp2641.scunet_login.manager

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

object HttpClientManager {
    private val _client = HttpClient(OkHttp)
    val client: HttpClient
        get() = _client

    suspend fun get(url: String): String {
        return client.get(url).bodyAsText()
    }

    suspend fun post(url: String, body: Any): String {
        return client.post(url) {
            setBody(body)
        }.bodyAsText()
    }
}