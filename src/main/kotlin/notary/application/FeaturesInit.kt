package main.kotlin.notary.application

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import main.kotlin.notary.responses.ErrorResponse
import main.kotlin.notary.responses.exceptions.NotaryException
import main.kotlin.notary.services.SessionService
import org.slf4j.event.Level
import java.text.DateFormat

fun Application.installFeatures(
    jwtAuthenticationConfig: JWTAuthenticationConfig,
    sessionService: SessionService
) {
    fun validateJWT(jwtCredential: JWTCredential): Principal? {
        return try {
            // Check if the session exist
            val sessionId = jwtCredential.payload.getClaim("sessionId").asString()
            sessionService.getSession(sessionId)

            // Check if the organization
            val organizationName = jwtCredential.payload.getClaim("organization").asString()
            sessionService.getOrganization(organizationName)

            // Success
            JWTPrincipal(jwtCredential.payload)

        } catch (ex: Exception) {
            null
        }
    }

    install(Resources)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(Authentication) {
        jwt("token-auth") {
            verifier(jwtAuthenticationConfig.verifier)
            realm = jwtAuthenticationConfig.issuer
            validate { validateJWT(it) }
        }
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat(DateFormat.LONG)
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->

            val status = when (cause) {
                is NotaryException -> cause.status
                else -> HttpStatusCode.InternalServerError
            }

            call.respond(status, ErrorResponse(message = cause.message.toString()))
        }

        status(HttpStatusCode.UnsupportedMediaType) { call, cause ->
            call.respond(
                cause,
                ErrorResponse(message = "Request body needs to be of type 'application/json'.")
            )
        }

        status(HttpStatusCode.InternalServerError) { call, cause ->
            call.respond(
                cause,
                ErrorResponse(message = "Something wrong happened with the server.")
            )
        }

        status(HttpStatusCode.NotFound) { call, cause ->
            call.respond(cause, ErrorResponse(message = "Requested resource not found."))
        }

        status(HttpStatusCode.Unauthorized) { call, cause ->
            call.respond(
                cause,
                ErrorResponse(message = "You need to be logged in to access this resource.")
            )
        }
    }
}