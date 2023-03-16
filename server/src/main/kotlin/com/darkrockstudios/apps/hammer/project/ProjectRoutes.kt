package com.darkrockstudios.apps.hammer.project

import com.darkrockstudios.apps.hammer.plugins.ServerUserIdPrincipal
import com.darkrockstudios.apps.hammer.plugins.USER_AUTH_NAME
import com.darkrockstudios.apps.hammer.projects.ProjectsRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Application.projectRoutes() {
    routing {
        authenticate(USER_AUTH_NAME) {
            route("/project/{userId}/{projectName}") {
                hasProject()
            }
        }
    }
}

private fun Route.hasProject() {
    val projectsRepository: ProjectsRepository = get()

    get("/has_project") {
        val principal = call.principal<ServerUserIdPrincipal>()!!
        val projectName = call.parameters["projectName"]

        if (projectName == null) {
            call.respondText("Missing Argument: projectName", status = HttpStatusCode.BadRequest)
        } else {
            //projectsRepository.hasProject(principal.name, projectName)

            call.respondText("Authenticated as ${principal.id}")
        }
    }
}