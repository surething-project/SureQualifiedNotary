package main.kotlin.notary.responses.exceptions

import io.ktor.http.*

abstract class NotFoundException(override val message: String) : NotaryException(message, HttpStatusCode.NotFound)

