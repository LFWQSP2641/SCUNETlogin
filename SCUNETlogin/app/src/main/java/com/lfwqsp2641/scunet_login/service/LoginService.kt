package com.lfwqsp2641.scunet_login.service

import com.lfwqsp2641.scunet_login.data.dto.Account
import com.lfwqsp2641.scunet_login.helper.LegacyCampusRsaEncryptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.parseQueryString
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.URL
import javax.net.SocketFactory

object LoginConstants {
    const val mainUrl = "http://192.168.2.135/"
    const val loginPath = "eportal/InterFace.do?method=login"
    const val externalPortalHost = "123.123.123.123"
    const val maxRetryAttempts = 3
    const val retryBaseDelayMillis = 300L

    val serviceCodeMap = mapOf(
        "chinatelecom" to "%E7%94%B5%E4%BF%A1%E5%87%BA%E5%8F%A3",    // 电信出口
        "chinamobile" to "%E7%A7%BB%E5%8A%A8%E5%87%BA%E5%8F%A3",     // 移动出口
        "chinaunicom" to "%E8%81%94%E9%80%9A%E5%87%BA%E5%8F%A3",     // 联通出口
        "edunet" to "internet",                                      // 教育出口
    )
    const val httpHeaderUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0"
    const val httpHeaderContentType = "application/x-www-form-urlencoded; charset=UTF-8"
    const val httpHeaderAccept = "*/*"

    val queryStringExtractPatterns = listOf(
        Regex("""/index\\.jsp\\?([^'"#\\s>]+)""", RegexOption.IGNORE_CASE),
        Regex(
            """top\\.self\\.location\\.href\\s*=\\s*['"][^'"]*/index\\.jsp\\?([^'"]+)['"]""",
            RegexOption.IGNORE_CASE
        )
    )
}

sealed class LoginException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class UnsupportedService(val service: String) :
        LoginException("Unsupported service: $service")

    class MissingMacAddress : LoginException("MAC address not found in query string")
    class AlreadyLoggedIn : LoginException("Already logged in")
    class RiskControlTriggered : LoginException("Trigger risk control")
    class TooManyUsersOnline : LoginException("Too many users online")

    data class MissingRedirectLocation(val step: String) :
        LoginException("Redirect location is missing ($step)")

    data class UnexpectedRedirect(val location: String) :
        LoginException("Unexpected redirect location: $location")

    data class QueryStringExtractionFailed(val reason: String) : LoginException(reason)

    data class LoginFailed(val responseSnippet: String) :
        LoginException("Login failed: $responseSnippet")
}

class LoginService(
    customSocketFactory: SocketFactory? = null,
) {
    private val client = createClient(customSocketFactory)

    private fun createClient(socketFactory: SocketFactory?) = HttpClient(OkHttp) {
        engine {
            config {
                socketFactory?.let { socketFactory(it) }
            }
        }
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 5000
        }
    }

    suspend fun startLogin(account: Account) {
        runWithRetry {
            val queryString = getLoginQueryString()
            postLoginRequest(queryString, account)
        }
    }

    private suspend fun postLoginRequest(queryString: String, account: Account) {
        val loginPostUrl = resolveRedirectUrl(LoginConstants.mainUrl, LoginConstants.loginPath)
        val serviceCode = LoginConstants.serviceCodeMap[account.service]
            ?: throw LoginException.UnsupportedService(account.service)
        val mac = getMacFromQueryString(queryString)
            ?: throw LoginException.MissingMacAddress()

        val response = client.post(loginPostUrl) {
            applyDefaultHeaders()
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("userId", account.username)
                        append(
                            "password",
                            LegacyCampusRsaEncryptor.encryptedPassword(account.password, mac)
                        )
                        append("service", serviceCode)
                        append("queryString", queryString)
                        append("operatorPwd", "")
                        append("operatorUserId", "")
                        append("validcode", "")
                        append("passwordEncrypt", "true")
                    }
                )
            )
        }

        val responseData = response.bodyAsText()
        when {
            responseData.contains("\"result\":\"success\"", ignoreCase = true) -> return
            responseData.contains("\"message\":\"验证码错误.\"", ignoreCase = true) -> {
                throw LoginException.RiskControlTriggered()
            }

            responseData.contains(
                "\"message\":\"你使用的账号已达到同时在线用户数量上限!\"",
                ignoreCase = true
            ) -> {
                throw LoginException.TooManyUsersOnline()
            }

            else -> throw LoginException.LoginFailed(compactResponse(responseData))
        }
    }

    private suspend fun getLoginQueryString(): String {
        val response = client.get(LoginConstants.mainUrl) {
            applyDefaultHeaders()
        }

        val redirect1Location = resolveRedirectUrl(
            LoginConstants.mainUrl,
            response.headers[HttpHeaders.Location]
                ?: throw LoginException.MissingRedirectLocation("redirect1")
        )

        if (!redirect1Location.contains("eportal/redirectortosuccess.jsp", ignoreCase = true)) {
            throw LoginException.UnexpectedRedirect(redirect1Location)
        }

        var cookieHeader = mergeCookieHeader("", response.headers)

        val redirect1Response = client.get(redirect1Location) {
            applyDefaultHeaders(cookieHeader)
        }

        val redirect2Location = resolveRedirectUrl(
            redirect1Location,
            redirect1Response.headers[HttpHeaders.Location]
                ?: throw LoginException.MissingRedirectLocation("redirect2")
        )

        if (redirect2Location.contains("success.jsp", ignoreCase = true)) {
            throw LoginException.AlreadyLoggedIn()
        } else if (!redirect2Location.contains(
                LoginConstants.externalPortalHost,
                ignoreCase = true
            )
        ) {
            throw LoginException.UnexpectedRedirect(redirect2Location)
        }

        cookieHeader = mergeCookieHeader(cookieHeader, redirect1Response.headers)

        val redirect2Response = client.get(redirect2Location) {
            applyDefaultHeaders(cookieHeader)
        }

        val htmlContent = redirect2Response.bodyAsText()
        return extractLoginQueryString(htmlContent)
    }

    private fun extractLoginQueryString(htmlContent: String): String {
        val normalizedHtml = htmlContent.replace("&amp;", "&")
        LoginConstants.queryStringExtractPatterns.forEach { regex ->
            regex.find(normalizedHtml)?.groupValues?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        val key = "/index.jsp?"
        val startIndex = normalizedHtml.indexOf(key)
        if (startIndex == -1) {
            throw LoginException.QueryStringExtractionFailed("Failed to extract query string from login page")
        }

        val valueStart = startIndex + key.length
        val endIndex = listOf(
            normalizedHtml.indexOf("'</script>", valueStart),
            normalizedHtml.indexOf('"', valueStart),
            normalizedHtml.indexOf('\'', valueStart),
            normalizedHtml.indexOf('<', valueStart),
            normalizedHtml.indexOf(' ', valueStart),
        )
            .filter { it > valueStart }
            .minOrNull()
            ?: normalizedHtml.length

        return normalizedHtml.substring(valueStart, endIndex).trim()
            .takeIf { it.isNotEmpty() }
            ?: throw LoginException.QueryStringExtractionFailed("Extracted query string is empty")
    }

    private fun getMacFromQueryString(queryString: String): String? {
        val parameters = parseQueryString(queryString)
        return parameters["mac"]
    }

    private fun mergeCookieHeader(existingCookieHeader: String, headers: Headers): String {
        val cookieMap = parseCookieHeader(existingCookieHeader)

        headers.getAll(HttpHeaders.SetCookie)
            .orEmpty()
            .asSequence()
            .map { it.substringBefore(';').trim() }
            .filter { '=' in it }
            .forEach { cookiePair ->
                val index = cookiePair.indexOf('=')
                if (index <= 0) return@forEach
                val cookieName = cookiePair.substring(0, index).trim()
                val cookieValue = cookiePair.substring(index + 1).trim()
                if (cookieName.isNotEmpty()) {
                    cookieMap[cookieName] = cookieValue
                }
            }

        return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun parseCookieHeader(cookieHeader: String): LinkedHashMap<String, String> {
        val cookieMap = linkedMapOf<String, String>()
        if (cookieHeader.isBlank()) {
            return cookieMap
        }

        cookieHeader
            .split(';')
            .map { it.trim() }
            .filter { '=' in it }
            .forEach { cookiePair ->
                val index = cookiePair.indexOf('=')
                if (index <= 0) return@forEach
                val cookieName = cookiePair.substring(0, index).trim()
                val cookieValue = cookiePair.substring(index + 1).trim()
                if (cookieName.isNotEmpty()) {
                    cookieMap[cookieName] = cookieValue
                }
            }

        return cookieMap
    }

    private fun HttpRequestBuilder.applyDefaultHeaders(cookieHeader: String? = null) {
        header(HttpHeaders.UserAgent, LoginConstants.httpHeaderUserAgent)
        header(HttpHeaders.ContentType, LoginConstants.httpHeaderContentType)
        header(HttpHeaders.Accept, LoginConstants.httpHeaderAccept)
        if (!cookieHeader.isNullOrBlank()) {
            header(HttpHeaders.Cookie, cookieHeader)
        }
    }

    private fun resolveRedirectUrl(baseUrl: String, location: String): String {
        return URL(URL(baseUrl), location).toString()
    }

    private suspend fun <T> runWithRetry(block: suspend () -> T): T {
        var attempt = 1
        var delayMillis = LoginConstants.retryBaseDelayMillis

        while (true) {
            try {
                return block()
            } catch (error: Throwable) {
                val isLastAttempt = attempt >= LoginConstants.maxRetryAttempts
                if (!isTransientNetworkError(error) || isLastAttempt) {
                    throw error
                }

                delay(delayMillis)
                delayMillis *= 2
                attempt++
            }
        }
    }

    private fun isTransientNetworkError(error: Throwable): Boolean {
        if (error is IOException) {
            return true
        }

        return error.cause?.let { isTransientNetworkError(it) } == true
    }

    private fun compactResponse(response: String): String {
        return response
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(256)
    }
}