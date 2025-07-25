package com.iggdrasil.ps_web_socket_service.application.config

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
@Component
class SimpleWebSocketHandler : WebSocketHandler {
    private val log = LoggerFactory.getLogger(SimpleWebSocketHandler::class.java)
    private val sessionsPerTenant = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val queryParams = UriComponentsBuilder.fromUri(session.handshakeInfo.uri).build().queryParams
        val tenant = queryParams.getFirst("tenant")

        if (tenant.isNullOrBlank()) {
            log.warn("Tenant not provided in WebSocket connection")
            return session.close(CloseStatus.BAD_DATA)
        }

        val tenantSessions = sessionsPerTenant.computeIfAbsent(tenant) { mutableListOf() }
        tenantSessions.add(session)
        log.info("New session for tenant [$tenant]: ${session.id}")

        return session.receive()
            .doOnNext { message ->
                val text = message.payloadAsText
                log.info("Message from [$tenant]: $text")

                tenantSessions.forEach {
                    if (it.isOpen) {
                        it.send(Mono.just(it.textMessage(text))).subscribe()
                    }
                }
            }
            .doFinally {
                log.info("Session closed for [$tenant]: ${session.id}")
                tenantSessions.remove(session)
                if (tenantSessions.isEmpty()) {
                    sessionsPerTenant.remove(tenant)
                }
            }
            .then()
    }
}
