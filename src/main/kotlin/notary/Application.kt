package main.kotlin.notary

import io.ktor.server.application.*
import io.ktor.server.routing.*
import main.kotlin.notary.application.JWTAuthenticationConfig
import main.kotlin.notary.application.initDatabase
import main.kotlin.notary.application.installFeatures
import main.kotlin.notary.controllers.*
import main.kotlin.notary.repositories.*
import main.kotlin.notary.services.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val jwtAuthenticationConfig = JWTAuthenticationConfig(environment)

    val organizationRepository = OrganizationRepository()
    val sessionRepository = SessionRepository()
    val bluetoothRepository = BluetoothRepository()
    val wiFiRepository = WiFiRepository()
    val citizenCardRepository = CitizenCardRepository()

    val organizationService = OrganizationService(organizationRepository)
    val sessionService = SessionService(organizationService, sessionRepository)
    val bluetoothService = BluetoothService(bluetoothRepository)
    val wiFiService = WiFiService(wiFiRepository)
    val citizenCardService = CitizenCardService(citizenCardRepository)
    val validationService = ValidationService(sessionService, organizationService, bluetoothService, wiFiService, citizenCardService)

    initDatabase(sessionService)

    installFeatures(jwtAuthenticationConfig, sessionService)

    routing {
        home()
        validations(validationService = validationService)
    }
}