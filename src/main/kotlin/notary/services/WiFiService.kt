package main.kotlin.notary.services

import main.kotlin.notary.models.locationProofs.NearbyWiFiNetworksEntity
import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.models.locationProofs.CitizenCardEntity
import main.kotlin.notary.repositories.WiFiRepository

class WiFiService(
    private val wiFiRepository: WiFiRepository,
) {
    fun getWiFiProofs(session: SessionEntity): List<NearbyWiFiNetworksEntity> {
        return wiFiRepository.getWiFiProofs(session)
    }

    fun validateProof(proof: NearbyWiFiNetworksEntity) {
        return wiFiRepository.validateProof(proof)
    }
}