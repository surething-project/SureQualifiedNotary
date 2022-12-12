package main.kotlin.notary.services

import main.kotlin.notary.models.locationProofs.CitizenCardEntity
import main.kotlin.notary.models.locationProofs.CitizenCardSignedEntity
import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.models.locationProofs.NearbyBluetoothDevicesEntity
import main.kotlin.notary.repositories.CitizenCardRepository

class CitizenCardService(
    private val citizenCardRepository: CitizenCardRepository,
) {
    fun getCitizenCardProofs(session: SessionEntity): List<CitizenCardEntity> {
        return citizenCardRepository.getCitizenCardProofs(session)
    }

    fun getCitizenCardSignedProofs(session: SessionEntity): List<CitizenCardSignedEntity> {
        return citizenCardRepository.getCitizenCardSignedProofs(session)
    }

    fun validateProof(proof: CitizenCardEntity) {
        return citizenCardRepository.validateProof(proof)
    }

    fun validateProof(proof: CitizenCardSignedEntity) {
        return citizenCardRepository.validateProof(proof)
    }
}