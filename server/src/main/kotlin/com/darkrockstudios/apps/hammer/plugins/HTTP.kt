package com.darkrockstudios.apps.hammer.plugins

import com.darkrockstudios.apps.hammer.ServerConfig
import com.darkrockstudios.apps.hammer.base.BuildMetadata
import com.darkrockstudios.apps.hammer.base.http.HAMMER_PROTOCOL_HEADER
import com.darkrockstudios.apps.hammer.base.http.HAMMER_PROTOCOL_VERSION
import com.darkrockstudios.apps.hammer.base.http.HEADER_SERVER_VERSION
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.httpsredirect.*
import io.ktor.server.routing.*

fun Application.configureHTTP(config: ServerConfig) {
	install(DefaultHeaders) {
		header(HAMMER_PROTOCOL_HEADER, HAMMER_PROTOCOL_VERSION.toString())
		header(HEADER_SERVER_VERSION, BuildMetadata.APP_VERSION)
	}
	install(IgnoreTrailingSlash)
	install(Compression) {
		gzip {
			priority = 1.0
		}
		deflate {
			priority = 10.0
			minimumSize(1024) // condition
		}
	}

	if (config.sslCert?.forceHttps == true) {
		install(HttpsRedirect)
	}
}
