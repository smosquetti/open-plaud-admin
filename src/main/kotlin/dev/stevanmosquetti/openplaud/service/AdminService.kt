package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import org.springframework.stereotype.Service

/**
 * Verifies admin access against ejabberd.
 */
@Service
class AdminService(
    private val ejabberd: EjabberdClient
) {

    /**
     * Verifies the caller is the configured admin account and its credentials pass
     * `check_password`.
     */
    fun checkAdminAccess(): Unit {
        ejabberd.checkAdminAccess()
    }
}