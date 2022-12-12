package main.kotlin.notary.controllers

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import kotlinx.html.*
import main.kotlin.notary.application.Home

fun Route.home() {
    get<Home> {
        call.respondHtml {
            head {
                meta(charset = "UTF-8")
                title { +"Notary" }
            }
            body {
                h1 {
                    +"Notary | Coming Soon ..."
                }
            }
        }
    }
}