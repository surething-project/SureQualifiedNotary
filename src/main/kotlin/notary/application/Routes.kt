package main.kotlin.notary.application

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource(homePath)
class Home
const val homePath = "/"

@Serializable
@Resource(createProofNoAuth)
class CreateProofNoAuth
const val createProofNoAuth = "/proof/create/{senderId}/noAuth"

@Serializable
@Resource(createProof)
class CreateProof
const val createProof = "/proof/create"
