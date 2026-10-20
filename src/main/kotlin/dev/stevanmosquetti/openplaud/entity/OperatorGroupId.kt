package dev.stevanmosquetti.openplaud.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class OperatorGroupId(
    @Column(name = "operator_id", nullable = false)
    val operatorId: UUID,

    @Column(name = "group_id", nullable = false)
    val groupId: UUID,
) : Serializable
