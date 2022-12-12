package main.kotlin.notary.repositories

import main.kotlin.notary.models.locationProofs.*
import main.kotlin.notary.models.SessionEntity
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class WiFiRepository {
    fun getWiFiProofs(session: SessionEntity) = transaction {
        NearbyWiFiNetworksEntity.find(NearbyWiFiNetworksTable.session eq session.id).filter {
                proof -> !proof.validated
        }
    }

    fun validateProof(proof: NearbyWiFiNetworksEntity) = transaction {
        proof.validated = true
    }
}