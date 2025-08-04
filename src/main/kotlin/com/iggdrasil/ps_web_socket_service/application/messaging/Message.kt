package com.iggdrasil.ps_web_socket_service.application.messaging

data class Message<T>(val tenant: String,val data: T?)