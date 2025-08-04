package com.iggdrasil.ps_web_socket_service.application.web.dto

data class ClientStatusDTO(
    val ip: String,
    val host: String,
    val online: Boolean
)