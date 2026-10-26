package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.Group
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data repository for the `groups` table.
 */
interface GroupRepository : JpaRepository<Group, UUID>
