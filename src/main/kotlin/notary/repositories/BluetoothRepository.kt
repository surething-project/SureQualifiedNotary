package main.kotlin.notary.repositories

import main.kotlin.notary.models.locationProofs.NearbyBluetoothDevicesEntity
import main.kotlin.notary.models.locationProofs.NearbyBluetoothDevicesTable
import main.kotlin.notary.models.OrganizationEntity
import main.kotlin.notary.models.Organizations
import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.responses.exceptions.organization.OrganizationNotFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BluetoothRepository {
    fun getBluetoothProofs(session: SessionEntity) = transaction {
        NearbyBluetoothDevicesEntity.find(NearbyBluetoothDevicesTable.session eq session.id).filter {
            proof -> !proof.validated
        }
    }

    fun validateProof(proof: NearbyBluetoothDevicesEntity) = transaction {
        proof.validated = true
    }
}