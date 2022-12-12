package main.kotlin.notary.responses.exceptions.proofType

import main.kotlin.notary.responses.exceptions.NotFoundException

data class ProofTypeNotFoundException(val name: String) : NotFoundException("Proof Type with name '$name' not found")