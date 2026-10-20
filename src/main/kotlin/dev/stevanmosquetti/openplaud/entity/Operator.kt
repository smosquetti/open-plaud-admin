package dev.stevanmosquetti.openplaud.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "operators")
class Operator(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String
)
