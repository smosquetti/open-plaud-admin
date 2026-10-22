package dev.stevanmosquetti.openplaud.repository

import dev.stevanmosquetti.openplaud.entity.OperatorGroup
import dev.stevanmosquetti.openplaud.entity.OperatorGroupId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface OperatorGroupRepository : JpaRepository<OperatorGroup, OperatorGroupId> {

    @Query("""
       SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END  
       FROM OperatorGroup o WHERE o.id.operatorId = :operatorId AND o.id.groupId = :groupId
    """)
    fun existsByOperatorIdAndGroupId(operatorId: UUID, groupId: UUID): Boolean

    @Query("""
       SELECT o.id.operatorId 
       FROM OperatorGroup o WHERE o.id.groupId = :groupId
    """)
    fun listOperatorsByGroupId(groupId: UUID): List<UUID>

    @Modifying
    @Query("DELETE FROM OperatorGroup o WHERE o.id.groupId = :groupId")
    fun deleteByGroup(groupId: UUID)

    @Modifying
    @Query("DELETE FROM OperatorGroup o WHERE o.id.operatorId = :operatorId")
    fun deleteByOperator(operatorId: UUID)
}
