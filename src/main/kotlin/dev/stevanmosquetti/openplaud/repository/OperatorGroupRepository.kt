package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.Group
import dev.stevanmosquetti.openplaud.entity.Operator
import dev.stevanmosquetti.openplaud.entity.OperatorGroup
import dev.stevanmosquetti.openplaud.entity.OperatorGroupId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
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
     * Returns all operators that belong to the group.
     */
    @Query("""
       SELECT o.id, o.name, o.username, o.email
       FROM operators o
       JOIN operator_groups og ON og.operator_id = o.id
       WHERE og.group_id = :groupId
    """, nativeQuery = true)
    fun listOperatorsByGroupId(groupId: UUID): List<Operator>

    /**
     * Returns all groups that an operator belongs to.
     */
    @Query("""
       SELECT g.id, g.name, g.node, g.service
       FROM groups g
       JOIN operator_groups og ON og.group_id = g.id
       WHERE og.operator_id = :operatorId
    """, nativeQuery = true)
    fun listGroupsByOperatorId(operatorId: UUID): List<Group>

    /**
     * Deletes all membership rows of the group.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OperatorGroup o WHERE o.id.groupId = :groupId")
    fun deleteByGroup(groupId: UUID)

    /**
     * Deletes all membership rows of the operator.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OperatorGroup o WHERE o.id.operatorId = :operatorId")
    fun deleteByOperator(operatorId: UUID)
}
