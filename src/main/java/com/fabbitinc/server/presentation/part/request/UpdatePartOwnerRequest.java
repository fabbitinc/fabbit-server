package com.fabbitinc.server.presentation.part.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "요청 DTO")
public class UpdatePartOwnerRequest {

    private UUID ownerId;
    private UUID ownerTeamId;
    private boolean ownerIdSet;
    private boolean ownerTeamIdSet;

    public UUID getOwnerId() {
        return ownerId;
    }

    @JsonSetter("owner_id")
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        this.ownerIdSet = true;
    }

    public UUID getOwnerTeamId() {
        return ownerTeamId;
    }

    @JsonSetter("owner_team_id")
    public void setOwnerTeamId(UUID ownerTeamId) {
        this.ownerTeamId = ownerTeamId;
        this.ownerTeamIdSet = true;
    }

    public boolean isOwnerIdSet() {
        return ownerIdSet;
    }

    public boolean isOwnerTeamIdSet() {
        return ownerTeamIdSet;
    }
}
