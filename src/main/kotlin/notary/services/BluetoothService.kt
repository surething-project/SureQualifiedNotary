package main.kotlin.notary.services

import main.kotlin.notary.models.locationProofs.NearbyBluetoothDevicesEntity
import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.repositories.BluetoothRepository

class BluetoothService(
    private val bluetoothRepository: BluetoothRepository,
) {
    fun getBluetoothProofs(session: SessionEntity): List<NearbyBluetoothDevicesEntity> {
        return bluetoothRepository.getBluetoothProofs(session)
    }

    fun validateProof(proof: NearbyBluetoothDevicesEntity) {
        return bluetoothRepository.validateProof(proof)
    }
}