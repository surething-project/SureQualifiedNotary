package main.kotlin.notary.repositories

import main.kotlin.notary.models.OrganizationEntity
import main.kotlin.notary.models.Organizations
import main.kotlin.notary.responses.exceptions.organization.OrganizationNotFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

class OrganizationRepository {

    fun getOrganization(name: String) = transaction {
        OrganizationEntity.find(Organizations.name eq name).firstOrNull() ?:
            throw OrganizationNotFoundException(name)
    }

    fun getOrganizationPolicy(name: String) = transaction {
        val organization = getOrganization(name)
        organization.policy.proofsType.split(" ")
    }

    fun getOrganizationPublicKey(name: String) = transaction {
        val organization = getOrganization(name)

        val encoded: ByteArray = Base64.getDecoder().decode(organization.publicKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(encoded)
        keyFactory.generatePublic(keySpec) as PublicKey
    }

    fun getOrganizationPrivateKey(name: String) = transaction {
        val organization = getOrganization(name)

        val encoded: ByteArray = Base64.getDecoder().decode(organization.privateKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        keyFactory.generatePrivate(keySpec) as PrivateKey
    }
}