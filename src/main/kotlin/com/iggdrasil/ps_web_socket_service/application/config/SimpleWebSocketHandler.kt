package com.iggdrasil.ps_web_socket_service.application.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class SimpleWebSocketHandler : WebSocketHandler {
    private val log = LoggerFactory.getLogger(SimpleWebSocketHandler::class.java)
    private val sessionsPerTenant = ConcurrentHashMap<String, MutableList<WebSocketSession>>()
    private val objectMapper = ObjectMapper()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val path = session.handshakeInfo.uri.path
        val tenant = path.substringAfterLast("/")

        if (tenant.isBlank()) {
            return session.close(CloseStatus.BAD_DATA)
        }

        val tenantSessions = sessionsPerTenant.computeIfAbsent(tenant) { mutableListOf() }
        tenantSessions.add(session)

        return session.receive()
            .doOnNext { message ->
                val text = message.payloadAsText
                broadcastToTenant(tenant, text)
            }
            .doFinally {
                tenantSessions.remove(session)
                if (tenantSessions.isEmpty()) {
                    sessionsPerTenant.remove(tenant)
                }
            }
            .then()
    }

    fun broadcastToTenant(tenant: String, data: Any?) {
        val tenantSessions = sessionsPerTenant[tenant] ?: return

        val message = when (data) {
            is String -> data
            else -> objectMapper.writeValueAsString(data)
        }

        tenantSessions.removeIf { session ->
            if (session.isOpen) {
                try {
                    session.send(Mono.just(session.textMessage(message))).subscribe()
                    false
                } catch (e: Exception) {
                    log.error("Failed to send message to session ${session.id}", e)
                    true
                }
            } else {
                true
            }
        }

        if (tenantSessions.isEmpty()) {
            sessionsPerTenant.remove(tenant)
        }
    }

    fun getActiveTenants(): Set<String> = sessionsPerTenant.keys.toSet()

    fun getSessionCountForTenant(tenant: String): Int = sessionsPerTenant[tenant]?.size ?: 0
}