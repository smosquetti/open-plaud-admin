package dev.stevanmosquetti.openplaud.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * JPA entity for the `operator_groups` table.
 */
@Entity
@Table(name = "operator_groups")
class OperatorGroup(
    @EmbeddedId
    val id: OperatorGroupId,
) {
    val operatorId get() = id.operatorId
    val groupId get() = id.groupId
}
