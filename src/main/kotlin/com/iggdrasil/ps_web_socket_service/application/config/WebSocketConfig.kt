package com.iggdrasil.ps_web_socket_service.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
@EnableWebFlux
class WebSocketConfig(private val handler: SimpleWebSocketHandler) : WebFluxConfigurer {

    @Bean
    fun handlerMapping(): SimpleUrlHandlerMapping {
        val map = mapOf("/ws/status" to handler)
        return SimpleUrlHandlerMapping().apply {
            order = -1
            urlMap = map
        }
    }

    @Bean
    fun handlerAdapter() = WebSocketHandlerAdapter()
}