package main.kotlin.notary.application

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.surething_project.signature.util.SignatureManager
import io.ktor.server.application.*
import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.models.locationProofs.CitizenCardEntity
import main.kotlin.notary.models.locationProofs.CitizenCardSignedEntity
import main.kotlin.notary.models.locationProofs.NearbyBluetoothDevicesEntity
import main.kotlin.notary.models.locationProofs.NearbyWiFiNetworksEntity
import main.kotlin.notary.security.SecurityUtils.getKioskPrivateKey
import main.kotlin.notary.security.SecurityUtils.getProverPrivateKey
import main.kotlin.notary.services.SessionService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import test.kotlin.utils.DEFAULT_SENDER_ID
import test.kotlin.utils.DEFAULT_STRING
import java.io.File
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

const val HIKARI_CONFIG_KEY = "ktor.hikariconfig"
const val TESTING_KEY = "ktor.testing"

fun Application.initDatabase(
    sessionService: SessionService
) {
    if (System.getenv("HEROKU_POSTGRESQL_COBALT_URL") != null) {
        val databaseURI = URI(System.getenv("HEROKU_POSTGRESQL_COBALT_URL"))
        val databaseUser = databaseURI.userInfo.split(":")[0]
        val databasePass = databaseURI.userInfo.split(":")[1]
        val databaseUrl = "jdbc:postgresql://" + databaseURI.host + ':' + databaseURI.port + databaseURI.path

        Database.connect(databaseUrl, user = databaseUser, password = databasePass)

    } else {
        val configPath = environment.config.property(HIKARI_CONFIG_KEY).getString()
        val dbConfig = HikariConfig(configPath)
        val dataSource = HikariDataSource(dbConfig)
        Database.connect(dataSource)
    }

    LoggerFactory.getLogger(Application::class.simpleName).info("Connected to the Database")

    val testing = environment.config.property(TESTING_KEY).getString().toBoolean()
    if (testing) {
        populateTables(sessionService)
        LoggerFactory.getLogger(Application::class.simpleName).info("Database Initialized")
    }
}

private fun populateTables(sessionService: SessionService) = transaction {
    val session = sessionService.getDefaultSession()
    val kioskKey = getKioskPrivateKey()
    val proverKey = getProverPrivateKey()

    // WiFi Proofs
    createWiFiProofs(session, kioskKey)
    createWiFiProofs(session, proverKey)

    // Bluetooth Proofs
    createBluetoothProofs(session, kioskKey)
    createBluetoothProofs(session, proverKey)

    // Citizen Card Proofs
    createCitizenCardProofs(session, kioskKey)
    createCitizenCardProofs(session, proverKey)

    // Citizen Card Signed Proofs
    createCitizenCardSignedProofs(session, kioskKey)
    createCitizenCardSignedProofs(session, proverKey)
}

private fun createWiFiProofs(session: SessionEntity, privateKey: PrivateKey) = transaction {
    NearbyWiFiNetworksEntity.new {
        this.wiFiNetworks = DEFAULT_STRING
        this.senderId = DEFAULT_SENDER_ID
        this.signature = createSignature(DEFAULT_STRING, privateKey)
        this.session = session
        this.validated = false
    }
}

private fun createBluetoothProofs(session: SessionEntity, privateKey: PrivateKey) = transaction {
    NearbyBluetoothDevicesEntity.new {
        this.bluetoothAps = DEFAULT_STRING
        this.senderId = DEFAULT_SENDER_ID
        this.signature = createSignature(DEFAULT_STRING, privateKey)
        this.session = session
        this.validated = false
    }
}

private fun createCitizenCardProofs(session: SessionEntity, privateKey: PrivateKey) = transaction {
    CitizenCardEntity.new {
        this.citizenCard = DEFAULT_STRING
        this.senderId = DEFAULT_SENDER_ID
        this.signature = createSignature(DEFAULT_STRING, privateKey)
        this.session = session
        this.validated = false
    }
}

private fun createCitizenCardSignedProofs(session: SessionEntity, privateKey: PrivateKey) = transaction {
    CitizenCardSignedEntity.new {
        this.citizenCard = DEFAULT_STRING
        this.citizenCardSignature = createSignature(session.sessionId, privateKey)
        this.senderId = DEFAULT_SENDER_ID
        this.signature = createSignature(DEFAULT_STRING, privateKey)
        this.session = session
        this.validated = false
    }
}

private fun createSignature(data: String, privateKey: PrivateKey): ByteArray {
    return SignatureManager.sign(data.toByteArray(), privateKey)
}