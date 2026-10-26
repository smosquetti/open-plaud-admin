package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.Operator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

/**
 * Spring Data repository for the `operators` table.
 */
interface OperatorRepository : JpaRepository<Operator, UUID> {

    /**
     * Returns the XMPP username (localpart) of the operator.
     */
    @Query("""
       SELECT o.username
       FROM Operator o WHERE o.id = :operatorId
    """)
    fun getUsernameById(operatorId: UUID): String
}
