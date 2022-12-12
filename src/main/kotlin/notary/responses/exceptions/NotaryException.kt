package main.kotlin.notary.responses.exceptions

import io.ktor.http.*

abstract class NotaryException(override val message: String, val status: HttpStatusCode) : RuntimeException(message)