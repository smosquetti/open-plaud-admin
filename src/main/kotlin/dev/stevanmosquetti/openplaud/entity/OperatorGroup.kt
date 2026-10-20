package dev.stevanmosquetti.openplaud.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "operator_groups")
class OperatorGroup(
    @EmbeddedId
    val id: OperatorGroupId,
) {
    val operatorId get() = id.operatorId
    val groupId get() = id.groupId
}
