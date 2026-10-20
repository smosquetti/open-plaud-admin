package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.Operator
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OperatorRepository : JpaRepository<Operator, UUID>
