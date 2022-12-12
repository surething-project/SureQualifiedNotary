package main.kotlin.notary.repositories

import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.models.Sessions
import main.kotlin.notary.responses.exceptions.session.SessionNotFinishedException
import main.kotlin.notary.responses.exceptions.session.SessionNotFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class SessionRepository {
    fun getDefaultSession() = transaction {
        SessionEntity.find(Sessions.sessionId.eq("default")).firstOrNull() ?:
        throw SessionNotFoundException("default")
    }

    fun getSession(sessionId: String) = transaction {
        SessionEntity.find(Sessions.sessionId.eq(sessionId)).firstOrNull() ?:
            throw SessionNotFoundException(sessionId)
    }

    fun getFinishedSession(sessionId: String) = transaction {
        val session = getSession(sessionId)
        if (!session.finished) throw SessionNotFinishedException(sessionId)

        session
    }
}

data class SessionData(var sessionId: String, val proverId: String)