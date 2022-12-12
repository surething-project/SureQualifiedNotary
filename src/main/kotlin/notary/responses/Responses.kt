package main.kotlin.notary.responses

data class SuccessResponse(val data: Any? = null, val token: String? = null)
data class ErrorResponse(val message: String)
