package main.kotlin.notary.responses.exceptions.policy

import main.kotlin.notary.responses.exceptions.NotFoundException

data class PolicyNotFoundException(val proofs: String) : NotFoundException("Policy with proofs '$proofs' not found")