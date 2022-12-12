package main.kotlin.notary.responses.exceptions.session

import main.kotlin.notary.responses.exceptions.DuplicateException

data class SessionNotFinishedException(val id: String) : DuplicateException("Session with id '$id' not finished yet")