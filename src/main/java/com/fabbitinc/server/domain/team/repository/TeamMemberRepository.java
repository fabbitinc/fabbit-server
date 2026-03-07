package com.fabbitinc.server.domain.team.repository;

import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    long countByTeam(Team team);

    List<TeamMember> findByTeam(Team team);

    List<TeamMember> findByTeamAndUserIdIn(Team team, Collection<UUID> userIds);

    int deleteByTeamAndUserIdIn(Team team, Collection<UUID> userIds);

    List<TeamMember> findByTeam_IdIn(Collection<UUID> teamIds);
}
