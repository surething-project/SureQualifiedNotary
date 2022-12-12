package main.kotlin.notary.services

import eu.surething_project.core.Signature
import eu.surething_project.signature.util.SignatureManager
import io.ktor.util.*
import main.kotlin.notary.controllers.SenderAndSessionData
import main.kotlin.notary.models.OrganizationEntity
import main.kotlin.notary.models.locationProofs.CitizenCardEntity
import main.kotlin.notary.models.locationProofs.CitizenCardSignedEntity
import main.kotlin.notary.models.locationProofs.NearbyBluetoothDevicesEntity
import main.kotlin.notary.models.locationProofs.NearbyWiFiNetworksEntity
import main.kotlin.notary.security.SecurityUtils
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import pt.ulisboa.tecnico.qscd.contract.PolicyProto.ProofsType.PROOF_TYPE
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

const val WIFI_PROOF = "Wifi Proof"
const val BLUETOOTH_PROOF = "Bluetooth Proof"
const val CITIZEN_CARD_PROOF = "Citizen Card Proof"
const val CITIZEN_CARD_SIGNED_PROOF = "Citizen Card Signed Proof"
const val ORGANIZATION = "Organization"
const val PROVER = "Prover"

class ValidationService(
    private val sessionService: SessionService,
    private val organizationService: OrganizationService,
    private val bluetoothService: BluetoothService,
    private val wiFiService: WiFiService,
    private val citizenCardService: CitizenCardService
) {
    private val kioskPublicKey = SecurityUtils.getPublicKey("kiosk_public_key.pem")
    private val proverPublicKey = SecurityUtils.getPublicKey("user_public_key.pem")
    private val inspectorPublicKey = SecurityUtils.getPublicKey("inspect_public_key.pem")
    private val transporterPublicKey = SecurityUtils.getPublicKey("transport_public_key.pem")

    fun createProof(senderId: String): ByteArray? {
        val proofsSuccess: MutableList<PROOF_TYPE> = mutableListOf()

        // Get information about the organization
        val organization = sessionService.getOrganization(senderId)
        val session = sessionService.getDefaultSession()

        // Get possible not validated proofs
        val bluetoothProofs = bluetoothService.getBluetoothProofs(session)
        val wiFiProofs = wiFiService.getWiFiProofs(session)
        val citizenCardProofs = citizenCardService.getCitizenCardProofs(session)

        // Validate Proofs
        if (bluetoothProofs.isNotEmpty()) validateBluetooth(bluetoothProofs, senderId, proofsSuccess)
        if (wiFiProofs.isNotEmpty()) validateWiFi(wiFiProofs, senderId, proofsSuccess)

        if (citizenCardProofs.isNotEmpty()) {
            val kiosk = citizenCardProofs.first()

            val signatureKiosk = validateSignature(kiosk.signature, kiosk.citizenCard, kioskPublicKey, CITIZEN_CARD_PROOF, ORGANIZATION)

            if (signatureKiosk) proofsSuccess.add(PROOF_TYPE.CITIZEN_CARD)
            citizenCardProofs.forEach { proof -> citizenCardService.validateProof(proof) }
        }

        // Create Signature
        return createSignature(organization, proofsSuccess)
    }

    fun createProof(senderAndSessionData: SenderAndSessionData): ByteArray? {
        val proofsSuccess: MutableList<PROOF_TYPE> = mutableListOf()

        // Get information about the organization
        val organization = sessionService.getOrganization(senderAndSessionData.senderId)
        val session = sessionService.getFinishedSession(senderAndSessionData.sessionId)

        // Get possible not validated proofs
        val bluetoothProofs = bluetoothService.getBluetoothProofs(session)
        val wiFiProofs = wiFiService.getWiFiProofs(session)
        val citizenCardProofs = citizenCardService.getCitizenCardProofs(session)
        val citizenCardSignedProofs = citizenCardService.getCitizenCardSignedProofs(session)

        // Validate Proofs
        if (bluetoothProofs.isNotEmpty()) validateBluetooth(bluetoothProofs, senderAndSessionData.senderId, proofsSuccess)
        if (wiFiProofs.isNotEmpty()) validateWiFi(wiFiProofs, senderAndSessionData.senderId, proofsSuccess)

        if (citizenCardProofs.isNotEmpty()) validateCitizenCard(citizenCardProofs, senderAndSessionData.senderId, proofsSuccess)
        else if (citizenCardSignedProofs.isNotEmpty()) validateCitizenCardSigned(citizenCardSignedProofs, senderAndSessionData, proofsSuccess)

        // Create Signature
        return createSignature(organization, proofsSuccess)
    }

    private fun validateBluetooth(bluetoothProofs: List<NearbyBluetoothDevicesEntity>, senderId: String, proofsSuccess: MutableList<PROOF_TYPE>) {
        val organizationIndex = if (bluetoothProofs[0].senderId == senderId ) 0 else 1
        val organization = bluetoothProofs[organizationIndex]
        val prover = bluetoothProofs[organizationIndex xor 1]

        val organizationKey = getOrganizationKey(organization.senderId)
        val proverKey = getProverKey(organization.senderId)

        val signatureOrganization = validateSignature(organization.signature, organization.bluetoothAps, organizationKey, BLUETOOTH_PROOF, ORGANIZATION)
        val signatureProver = validateSignature(prover.signature, prover.bluetoothAps, proverKey, BLUETOOTH_PROOF, PROVER)

        val nearbyDevices = compareNearbyProofs(organization.bluetoothAps, prover.bluetoothAps, BLUETOOTH_PROOF)

        if (signatureOrganization and signatureProver and nearbyDevices) proofsSuccess.add(PROOF_TYPE.BLUETOOTH_NEARBY_DEVICES)
        bluetoothProofs.forEach { proof -> bluetoothService.validateProof(proof) }
    }

    private fun validateWiFi(wiFiProofs: List<NearbyWiFiNetworksEntity>, senderId: String, proofsSuccess: MutableList<PROOF_TYPE>) {
        val organizationIndex = if (wiFiProofs[0].senderId == senderId ) 0 else 1
        val organization = wiFiProofs[organizationIndex]
        val prover = wiFiProofs[organizationIndex xor 1]

        val organizationKey = getOrganizationKey(organization.senderId)
        val proverKey = getProverKey(organization.senderId)

        val signatureOrganization = validateSignature(organization.signature, organization.wiFiNetworks, organizationKey, WIFI_PROOF, ORGANIZATION)
        val signatureProver = validateSignature(prover.signature, prover.wiFiNetworks, proverKey, WIFI_PROOF, PROVER)

        val nearbyNetworks = compareNearbyProofs(organization.wiFiNetworks, prover.wiFiNetworks, WIFI_PROOF)

        if (signatureOrganization and signatureProver and nearbyNetworks) proofsSuccess.add(PROOF_TYPE.WIFI_NEARBY_DEVICES)
        wiFiProofs.forEach { proof -> wiFiService.validateProof(proof) }
    }

    private fun validateCitizenCard(citizenCardProofs: List<CitizenCardEntity>, senderId: String, proofsSuccess: MutableList<PROOF_TYPE>) {
        val organizationIndex = if (citizenCardProofs[0].senderId == senderId ) 0 else 1
        val organization = citizenCardProofs[organizationIndex]
        val prover = citizenCardProofs[organizationIndex xor 1]

        val organizationKey = getOrganizationKey(organization.senderId)
        val proverKey = getProverKey(organization.senderId)

        val signatureOrganization = validateSignature(organization.signature, organization.citizenCard, organizationKey, CITIZEN_CARD_PROOF, ORGANIZATION)
        val signatureProver = validateSignature(prover.signature, prover.citizenCard, proverKey, CITIZEN_CARD_PROOF, PROVER)

        if (signatureOrganization and signatureProver) proofsSuccess.add(PROOF_TYPE.CITIZEN_CARD)
        citizenCardProofs.forEach { proof -> citizenCardService.validateProof(proof) }
    }

    private fun validateCitizenCardSigned(citizenCardSignedProofs: List<CitizenCardSignedEntity>, senderAndSessionData: SenderAndSessionData, proofsSuccess: MutableList<PROOF_TYPE>) {
        val organizationIndex = if (citizenCardSignedProofs[0].senderId == senderAndSessionData.senderId) 0 else 1
        val organization = citizenCardSignedProofs[organizationIndex]
        val prover = citizenCardSignedProofs[organizationIndex xor 1]

        val organizationKey = getOrganizationKey(organization.senderId)
        val proverKey = getProverKey(organization.senderId)

        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(organization.publicKey))
        val keyFactory = KeyFactory.getInstance("RSA")
        val ccPublicKey = keyFactory.generatePublic(keySpec) as PublicKey

        LoggerFactory.getLogger(ValidationService::class.simpleName).info("| Regular Signature |")
        val signatureOrganization = validateSignature(organization.signature, organization.citizenCard, organizationKey, CITIZEN_CARD_SIGNED_PROOF, ORGANIZATION)
        val signatureProver = validateSignature(prover.signature, prover.citizenCard, proverKey, CITIZEN_CARD_SIGNED_PROOF, PROVER)

        LoggerFactory.getLogger(ValidationService::class.simpleName).info("| Citizen Card Signature |")
        val citizenCardSignatureOrganization = validateSignature(organization.citizenCardSignature, senderAndSessionData.sessionId, ccPublicKey, CITIZEN_CARD_SIGNED_PROOF, ORGANIZATION)
        val citizenCardSignatureProver = validateSignature(organization.citizenCardSignature, senderAndSessionData.sessionId, ccPublicKey, CITIZEN_CARD_SIGNED_PROOF, PROVER)

        if (signatureOrganization and signatureProver and citizenCardSignatureOrganization and citizenCardSignatureProver) proofsSuccess.add(PROOF_TYPE.CITIZEN_CARD_SIGNED)
        citizenCardSignedProofs.forEach { proof -> citizenCardService.validateProof(proof) }
    }

    private fun createSignature(organization: OrganizationEntity, proofsSuccess: MutableList<PROOF_TYPE>): ByteArray? {
        // Get organization policies
        val policies = organizationService.getPolicy(organization.name)
        var pointsAchieved = 0
        var totalPoints = 0
        policies.proofsTypeList.forEach { policy ->
            if (policy.proofType in proofsSuccess ||
                policy.proofType.equals(PROOF_TYPE.QRCODE_SCAN) ||
                policy.proofType.equals(PROOF_TYPE.BLUETOOTH_CONNECTION)
            ) pointsAchieved += policy.points
            totalPoints += policy.points
        }

        val dataToSign = JSONObject()

        return try {
            dataToSign["verdict"] = "$pointsAchieved out of $totalPoints possible points"
            dataToSign["proofs succeeded"] = proofsSuccess

            val encoded: ByteArray = Base64.getDecoder().decode(organization.privateKey)
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = PKCS8EncodedKeySpec(encoded)

            SignatureManager.sign(dataToSign.toString().decodeBase64Bytes(), keyFactory.generatePrivate(keySpec) as PrivateKey)

        } catch (e: Exception) {
            println(e.message)
            null
        }
    }

    private fun getOrganizationKey(senderId: String): PublicKey {
        return if (senderId == "kiosk") {
            kioskPublicKey
        } else {
            inspectorPublicKey
        }
    }

    private fun getProverKey(senderId: String): PublicKey {
        return if (senderId == "kiosk") {
            proverPublicKey
        } else {
            transporterPublicKey
        }
    }
}

private fun compareNearbyProofs(organization: String, prover: String, proofType: String): Boolean {
    val organizationList = organization.split(" ")
    val proverList = prover.split(" ")
    LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: Organization Devices -> $organizationList.")
    LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: Prover Devices -> $proverList.")

    val comparison = organizationList.intersect(proverList.toHashSet())
    LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: Common Devices -> $comparison.")

    val approved = comparison.size >= organizationList.size / 2

    if (approved) LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: Nearby devices match.")
    else          LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: Nearby devices does not match.")

    return approved
}

private fun validateSignature(signature: ByteArray, message: String, publicKey: PublicKey, proofType: String, entity: String): Boolean {
    return try {
        val signatureProto = Signature.parseFrom(signature)
        val verify = SignatureManager.verify(signatureProto.value.toByteArray(), message.toByteArray(), publicKey)

        if (verify) LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: $entity signature match.")
        else        LoggerFactory.getLogger(ValidationService::class.simpleName).info("$proofType: $entity signature does not match | $message.")

        verify

    } catch (ex: Exception) {
        println(ex.localizedMessage)
        false
    }
}

