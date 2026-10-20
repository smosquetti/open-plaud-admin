package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.Group
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GroupRepository : JpaRepository<Group, UUID>
