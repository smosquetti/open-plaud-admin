package dev.stevanmosquetti.openplaud.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

/**
 * Composite id for the `operator_groups` table.
 */
@Embeddable
data class OperatorGroupId(
    @Column(name = "operator_id", nullable = false)
    val operatorId: UUID,

    @Column(name = "group_id", nullable = false)
    val groupId: UUID,
) : Serializable
