package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.dto.CreateGroupRequest
import dev.stevanmosquetti.openplaud.dto.GroupResponse
import dev.stevanmosquetti.openplaud.service.GroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class GroupController(
    private val groupService: GroupService,
) {

    /**
     * Creates a group: creates a persistent MUC room on ejabberd and inserts a row
     * into the `groups` table.
     */
    @PostMapping("/groups")
    fun createGroup(@Valid @RequestBody request: CreateGroupRequest): ResponseEntity<GroupResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(request.name, request.service))

    /**
     * Adds an operator to a group: sets the MUC affiliation to `member` on ejabberd
     * and inserts a row into `operator_groups`.
     */
    @PostMapping("/groups/{groupId}/operators/{operatorId}")
    fun addOperatorToGroup(
        @PathVariable groupId: UUID,
        @PathVariable operatorId: UUID,
    ): ResponseEntity<Unit> {
        groupService.addOperatorToGroup(groupId, operatorId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * Removes an operator from a group: sets the MUC affiliation to `none` on ejabberd
     * and deletes the `operator_groups` row.
     */
    @DeleteMapping("/groups/{groupId}/operators/{operatorId}")
    fun removeOperatorFromGroup(
        @PathVariable groupId: UUID,
        @PathVariable operatorId: UUID,
    ): ResponseEntity<Unit> {
        groupService.removeOperatorFromGroup(groupId, operatorId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    /**
     * Removes a group: remove the MUC on ejabberd
     * and deletes the `groups` row.
     */
    @DeleteMapping("/groups/{groupId}")
    fun removeRoom(@PathVariable groupId: UUID): ResponseEntity<Unit> {
        groupService.removeRoom(groupId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
