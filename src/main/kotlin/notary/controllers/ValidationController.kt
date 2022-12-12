package main.kotlin.notary.controllers

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import main.kotlin.notary.application.CreateProof
import main.kotlin.notary.application.CreateProofNoAuth
import main.kotlin.notary.responses.ErrorResponse
import main.kotlin.notary.responses.SuccessResponse
import main.kotlin.notary.services.*

fun Route.validations(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    validationService: ValidationService
) {
    post<CreateProofNoAuth> {
        withContext(dispatcher) {
            val senderId = call.parameters["senderId"] ?: throw NotFoundException("Sender Id is necessary")

            val proof = validationService.createProof(senderId)
            call.respond(SuccessResponse(data = proof))
        }
    }

    authenticate("token-auth") {
        post<CreateProof> {
            withContext(dispatcher) {
                val senderAndSessionData = getSenderAndSessionIds(call)

                val proof = validationService.createProof(senderAndSessionData)
                call.respond(SuccessResponse(data = proof))
            }
        }
    }
}

private fun getSenderAndSessionIds(call: ApplicationCall): SenderAndSessionData {
    val principal = call.principal<JWTPrincipal>()

    // Get the organization name / prover id
    val organizationName = principal!!.payload.getClaim("organization").asString()
    val sessionId = principal.payload.getClaim("sessionId").asString()

    return SenderAndSessionData(organizationName, sessionId)
}

data class SenderAndSessionData(val senderId: String, val sessionId: String)