package main.kotlin.notary.responses.exceptions.session

import main.kotlin.notary.responses.exceptions.NotFoundException

data class SessionNotFoundException(val id: String) : NotFoundException("Session with id '$id' not found")