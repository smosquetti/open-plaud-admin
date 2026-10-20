package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.Operator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface OperatorRepository : JpaRepository<Operator, UUID> {

    @Query("""
       SELECT o.username
       FROM Operator o WHERE o.id = :operatorId
    """)
    fun getUsernameById(operatorId: UUID): String
}
