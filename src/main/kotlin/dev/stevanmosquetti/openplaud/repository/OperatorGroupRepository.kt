package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.OperatorGroup
import dev.stevanmosquetti.openplaud.entity.OperatorGroupId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

/**
 * Spring Data repository for the `operator_groups` table.
 */
interface OperatorGroupRepository : JpaRepository<OperatorGroup, OperatorGroupId> {

    /**
     * True when the operator is already a member of the group.
     */
    @Query("""
       SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
       FROM OperatorGroup o WHERE o.id.operatorId = :operatorId AND o.id.groupId = :groupId
    """)
    fun existsByOperatorIdAndGroupId(operatorId: UUID, groupId: UUID): Boolean

    /**
     * Returns the ids of all operators that belong to the group.
     */
    @Query("""
       SELECT o.id.operatorId
       FROM OperatorGroup o WHERE o.id.groupId = :groupId
    """)
    fun listOperatorsByGroupId(groupId: UUID): List<UUID>

    /**
     * Deletes all membership rows of the group.
     */
    @Modifying
    @Query("DELETE FROM OperatorGroup o WHERE o.id.groupId = :groupId")
    fun deleteByGroup(groupId: UUID)

    /**
     * Deletes all membership rows of the operator.
     */
    @Modifying
    @Query("DELETE FROM OperatorGroup o WHERE o.id.operatorId = :operatorId")
    fun deleteByOperator(operatorId: UUID)
}
