package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val ejabberd: EjabberdClient
) {
    
    fun checkAdminAccess(): Unit {
        ejabberd.checkAdminAccess()
    }
}