package com.iggdrasil.ps_web_socket_service.application.messaging

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.iggdrasil.ps_web_socket_service.application.config.SimpleWebSocketHandler
import com.iggdrasil.ps_web_socket_service.application.web.dto.ClientStatusDTO
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class HealthCheckKafkaConsumer(
    private val objectMapper: ObjectMapper,
    private val webSocketHandler: SimpleWebSocketHandler
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [Topic.HEALTH_CHECK])
    fun handleHealthCheck(record: ConsumerRecord<String, String>) {
        try {
            val message = objectMapper.readValue(record.value(), object : TypeReference<Message<Any>>() {})

            val statusData = when (val data = message.data) {
                is LinkedHashMap<*, *> -> objectMapper.convertValue(data, ClientStatusDTO::class.java)
                is ClientStatusDTO -> data
                else -> {
                    logger.warn("Unknown data type: ${data?.javaClass?.simpleName}")
                    return
                }
            }

            // Envia para as sessões WebSocket do tenant
            webSocketHandler.broadcastToTenant(message.tenant, statusData)

            logger.debug("Health check message sent to tenant ${message.tenant}: $statusData")
        } catch (ex: Exception) {
            logger.error("Failed to process health check message", ex)
        }
    }
}