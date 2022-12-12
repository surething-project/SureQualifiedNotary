package main.kotlin.notary.responses.exceptions

import io.ktor.http.*

abstract class DuplicateException(override val message: String) : NotaryException(message, HttpStatusCode.Conflict)

