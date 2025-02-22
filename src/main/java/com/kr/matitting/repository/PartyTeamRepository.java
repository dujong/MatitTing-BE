package com.kr.matitting.repository;

import com.kr.matitting.constant.Role;
import com.kr.matitting.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyTeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByPartyId(Long partyId);

    List<Team> findByUserId(Long userId);

    List<Team> findByUserIdAndRole(Long userId, Role role);
}
