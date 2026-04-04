package com.lfwqsp2641.scunet_login.service

import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.helper.LegacyCampusRsaEncryptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.parseQueryString

object LoginConstants {
    const val mainUrl = "http://192.168.2.135/"
    val serviceCodeMap = mapOf(
        "chinatelecom" to "%E7%94%B5%E4%BF%A1%E5%87%BA%E5%8F%A3",    // 电信出口
        "chinamobile" to "%E7%A7%BB%E5%8A%A8%E5%87%BA%E5%8F%A3",     // 移动出口
        "chinaunicom" to "%E8%81%94%E9%80%9A%E5%87%BA%E5%8F%A3",     // 联通出口
        "edunet" to "internet",                                      // 教育出口
    )
    const val httpHeaderUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0"
    const val httpHeaderContentType = "application/x-www-form-urlencoded; charset=UTF-8"
    const val httpHeaderAccept = "*/*"
}

class LoginService {
    private var client = createClient()

    private fun createClient() = HttpClient(CIO) {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 5000
        }
    }

    suspend fun startLogin(account: Account) {
        val queryString = getLoginQueryString()
        postLoginRequest(queryString, account)
    }

    private suspend fun postLoginRequest(queryString: String, account: Account) {
        val loginPostUrl = "${LoginConstants.mainUrl}eportal/InterFace.do?method=login"
        val serviceCode = LoginConstants.serviceCodeMap[account.service]
            ?: throw Exception("Unsupported service: ${account.service}")
        val mac = getMacFromQueryString(queryString)
            ?: throw Exception("MAC address not found in query string")
        val loginPostData = mapOf(
            "userId" to account.username,
            "password" to LegacyCampusRsaEncryptor.encryptedPassword(account.password, mac),
            "service" to serviceCode,
            "queryString" to queryString,
            "operatorPwd" to "",
            "operatorUserId" to "",
            "validcode" to "",
            "passwordEncrypt" to "true"
        )
        val response = client.post(loginPostUrl) {
            header(HttpHeaders.UserAgent, LoginConstants.httpHeaderUserAgent)
            header(HttpHeaders.ContentType, LoginConstants.httpHeaderContentType)
            header(HttpHeaders.Accept, LoginConstants.httpHeaderAccept)
            setBody(loginPostData)
        }
        val responseData = response.bodyAsText()
        if (responseData.contains("\"result\":\"success\"", ignoreCase = true)) {
            return
        } else if (responseData.contains("\"message\":\"验证码错误.\"", ignoreCase = true)) {
            throw Exception("Trigger risk control")
        } else if (responseData.contains("\"message\":\"你使用的账号已达到同时在线用户数量上限!\"", ignoreCase = true)) {
            throw Exception("Too many users online")
        } else {
            throw Exception("Login failed: $responseData")
        }
    }

    private suspend fun getLoginQueryString(): String {
        val response = client.get(LoginConstants.mainUrl) {
            header(HttpHeaders.UserAgent, LoginConstants.httpHeaderUserAgent)
            header(HttpHeaders.ContentType, LoginConstants.httpHeaderContentType)
            header(HttpHeaders.Accept, LoginConstants.httpHeaderAccept)
        }

        val redirectLocation = response.headers[HttpHeaders.Location]
            ?: throw Exception("Redirect location is missing (expected login redirect)")

        if (redirectLocation.contains("success", ignoreCase = true)) {
            throw Exception("Already online (detected 'success' in redirect URL)")
        }

        val cookieHeader = response.headers.getAll(HttpHeaders.SetCookie)
            .orEmpty()
            .asSequence()
            .map { it.substringBefore(';').trim() }
            .filter { '=' in it }
            .joinToString("; ")

        val redirectResponse = client.get(redirectLocation) {
            header(HttpHeaders.UserAgent, LoginConstants.httpHeaderUserAgent)
            header(HttpHeaders.ContentType, LoginConstants.httpHeaderContentType)
            header(HttpHeaders.Accept, LoginConstants.httpHeaderAccept)
            if (cookieHeader.isNotEmpty()) {
                header(HttpHeaders.Cookie, cookieHeader)
            }
        }

        val htmlContent = redirectResponse.bodyAsText()
        return extractLoginQueryString(htmlContent)
    }

    private fun extractLoginQueryString(htmlContent: String): String {
        val regex = Regex("""/index\.jsp\?([^'"#\s>]+)""")
        regex.find(htmlContent)?.groupValues?.getOrNull(1)?.let { return it }

        val key = "/index.jsp?"
        val startIndex = htmlContent.indexOf(key)
        if (startIndex == -1) {
            throw Exception("Failed to extract query string from login page")
        }

        val valueStart = startIndex + key.length
        val endIndex = listOf(
            htmlContent.indexOf("'</script>", valueStart),
            htmlContent.indexOf('"', valueStart),
            htmlContent.indexOf('\'', valueStart),
            htmlContent.indexOf('<', valueStart),
            htmlContent.indexOf(' ', valueStart),
        )
            .filter { it > valueStart }
            .minOrNull()
            ?: htmlContent.length

        return htmlContent.substring(valueStart, endIndex).trim()
            .takeIf { it.isNotEmpty() }
            ?: throw Exception("Extracted query string is empty")
    }

    private fun getMacFromQueryString(queryString: String): String? {
        val parameters = parseQueryString(queryString)
        return parameters["mac"]
    }
}