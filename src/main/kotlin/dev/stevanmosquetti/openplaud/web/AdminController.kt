package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.service.AdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val adminService: AdminService,
) {

    /**
     * Verifies the caller's admin credentials against ejabberd `check_password`;
     * the user must be the configured admin account (`ejabberd.username`).
     */
    @GetMapping("/login")
    fun checkPassword(): ResponseEntity<Unit> {
        adminService.checkAdminAccess()
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
