package main.kotlin.notary.application

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*

class JWTAuthenticationConfig(
    environment: ApplicationEnvironment
) {
    private val secret = environment.config.property("jwt.secret").getString()
    private val algorithm = Algorithm.HMAC512(secret)

    val issuer = environment.config.property("jwt.issuer").getString()

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()
}