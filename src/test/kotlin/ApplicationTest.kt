package test.kotlin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import main.kotlin.notary.application.*
import org.junit.Test
import test.kotlin.utils.DEFAULT_SENDER_ID
import test.kotlin.utils.DEFAULT_SESSION
import java.util.*
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testServer() = testApplication {
        val response = client.get(homePath)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testServerHTTPS() = testApplication {
        val response = client.get(homePath) { url { protocol = URLProtocol.HTTPS } }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // ========================
    // = No Token Proof Tests =
    // ========================

    @Test
    fun testCreateProofNoAuth() = testApplication {
        val response = createPostRequest(client, createProofNoAuth, DEFAULT_SENDER_ID, "".toByteArray())
        assertEquals(HttpStatusCode.OK, response.status)
    }

    // =====================
    // = Token Proof Tests =
    // =====================

    @Test
    fun testCreateProof() = testApplication {
        val config = ApplicationConfig("application.conf")
        val token = createToken(config)

        val response = createAuthPostRequest(client, createProof, token, "".toByteArray())
        assertEquals(HttpStatusCode.OK, response.status)
    }
}

private fun createToken(config: ApplicationConfig): String {
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val validity = config.property("jwt.validity").getString().toInt()
    val algorithm = Algorithm.HMAC512(secret)

    return JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("sessionId", DEFAULT_SESSION)
        .withClaim("organization", DEFAULT_SENDER_ID)
        .withExpiresAt(Date(System.currentTimeMillis() + validity))
        .sign(algorithm)
}

private suspend fun createPostRequest(client: HttpClient, url: String, parameter: String, body: ByteArray): HttpResponse {
    val parameterSlice = "{" + url.substringAfter("{").substringBefore("}") + "}"

    return client.post(url.replace(parameterSlice, parameter)) {
        url { protocol = URLProtocol.HTTPS }
        contentType(ContentType.Application.ProtoBuf)
        setBody(body)
    }
}

private suspend fun createAuthPostRequest(client: HttpClient, url: String, token: String, body: ByteArray): HttpResponse {
    return client.post(url) {
        url { protocol = URLProtocol.HTTPS }
        contentType(ContentType.Application.ProtoBuf)
        bearerAuth(token)
        setBody(body)
    }
}