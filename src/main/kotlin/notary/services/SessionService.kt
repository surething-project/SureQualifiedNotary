package main.kotlin.notary.services

import main.kotlin.notary.models.OrganizationEntity
import main.kotlin.notary.models.SessionEntity
import main.kotlin.notary.repositories.SessionRepository

class SessionService(
    private val organizationService: OrganizationService,
    private val sessionRepository: SessionRepository
) {
    fun getDefaultSession(): SessionEntity {
        return sessionRepository.getDefaultSession()
    }

    fun getSession(sessionId: String): SessionEntity {
        return sessionRepository.getSession(sessionId)
    }

    fun getFinishedSession(sessionId: String): SessionEntity {
        return sessionRepository.getFinishedSession(sessionId)
    }

    fun getOrganization(organizationName: String): OrganizationEntity {
        return organizationService.getOrganization(organizationName)
    }
}