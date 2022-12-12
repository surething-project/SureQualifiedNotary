package main.kotlin.notary.services

import main.kotlin.notary.models.OrganizationEntity
import main.kotlin.notary.repositories.OrganizationRepository
import pt.ulisboa.tecnico.qscd.contract.PolicyProto.Policy
import pt.ulisboa.tecnico.qscd.contract.PolicyProto.ProofsType
import pt.ulisboa.tecnico.qscd.contract.PolicyProto.ProofsType.PROOF_TYPE
import java.security.PrivateKey
import java.security.PublicKey

class OrganizationService(
    private val organizationRepository: OrganizationRepository
) {
    fun getOrganization(organizationName: String): OrganizationEntity {
        return organizationRepository.getOrganization(organizationName)
    }

    fun getPolicy(organizationName: String): Policy {
        val proofsType = organizationRepository.getOrganizationPolicy(organizationName)

        val policyProto = Policy.newBuilder()
        for (proofType in proofsType) {
            val proofTypeEnum = PROOF_TYPE.valueOf(proofType.replace(",", ""))
            val proofTypeProto = ProofsType.newBuilder().setProofType(proofTypeEnum).build()
            policyProto.addProofsType(proofTypeProto)
        }

        return policyProto.build()
    }

    fun getPublicKey(organizationName: String): PublicKey {
        return organizationRepository.getOrganizationPublicKey(organizationName)
    }

    fun getPrivateKey(organizationName: String): PrivateKey {
        return organizationRepository.getOrganizationPrivateKey(organizationName)
    }
}