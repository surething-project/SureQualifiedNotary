package main.kotlin.notary.repositories

import main.kotlin.notary.models.locationProofs.*
import main.kotlin.notary.models.SessionEntity
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CitizenCardRepository {
    fun getCitizenCardProofs(session: SessionEntity) = transaction {
        CitizenCardEntity.find(CitizenCards.session eq session.id).filter {
                proof -> !proof.validated
        }
    }

    fun getCitizenCardSignedProofs(session: SessionEntity) = transaction {
        CitizenCardSignedEntity.find(CitizenCardsSigned.session eq session.id).filter {
                proof -> !proof.validated
        }
    }

    fun validateProof(proof: CitizenCardEntity) = transaction {
        proof.validated = true
    }

    fun validateProof(proof: CitizenCardSignedEntity) = transaction {
        proof.validated = true
    }
}