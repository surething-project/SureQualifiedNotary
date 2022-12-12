package main.kotlin.notary.responses.exceptions.organization

import main.kotlin.notary.responses.exceptions.NotFoundException

data class OrganizationNotFoundException(val name: String) : NotFoundException("Organization with name '$name' not found")