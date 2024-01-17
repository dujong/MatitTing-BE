package com.kr.matitting.service;

import com.kr.matitting.constant.*;
import com.kr.matitting.dto.*;
import com.kr.matitting.entity.Party;
import com.kr.matitting.entity.PartyJoin;
import com.kr.matitting.entity.Team;
import com.kr.matitting.entity.User;
import com.kr.matitting.exception.Map.MapException;
import com.kr.matitting.exception.Map.MapExceptionType;
import com.kr.matitting.exception.main.MainExceptionType;
import com.kr.matitting.exception.party.PartyException;
import com.kr.matitting.exception.party.PartyExceptionType;
import com.kr.matitting.exception.partyjoin.PartyJoinException;
import com.kr.matitting.exception.partyjoin.PartyJoinExceptionType;
import com.kr.matitting.exception.user.UserException;
import com.kr.matitting.exception.user.UserExceptionType;
import com.kr.matitting.repository.PartyJoinRepository;
import com.kr.matitting.repository.PartyRepository;
import com.kr.matitting.repository.PartyTeamRepository;
import com.kr.matitting.repository.UserRepository;
import com.kr.matitting.repository.PartyRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.geom.Point2D;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.kr.matitting.dto.ChatRoomDto.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PartyService {
    @Value("${cloud.aws.s3.url}")
    private String url;
    private final ApplicationEventPublisher eventPublisher;
    private final PartyJoinRepository partyJoinRepository;
    private final PartyTeamRepository teamRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final MapService mapService;
    public ResponsePartyDetailDto getPartyInfo(User user, Long partyId) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new PartyException(PartyExceptionType.NOT_FOUND_PARTY));
        increaseHit(partyId);
        return ResponsePartyDetailDto.builder()
                .userId(party.getUser().getId())
                .leaderTF(user == null || (user.getId() != party.getUser().getId()) ? false : true)
                .partyId(party.getId())
                .partyTitle(party.getPartyTitle())
                .partyContent(party.getPartyContent())
                .address(party.getAddress())
                .longitude(party.getLongitude())
                .latitude(party.getLatitude())
                .status(party.getStatus())
                .gender(party.getGender())
                .age(party.getAge())
                .deadline(party.getDeadline())
                .partyTime(party.getPartyTime())
                .totalParticipate(party.getTotalParticipant())
                .participate(party.getParticipantCount())
                .menu(party.getMenu())
                .category(party.getCategory())
                .thumbnail(party.getThumbnail())
                .hit(party.getHit())
                .build();
    }

    @Transactional
    public void increaseHit(Long partyId){
        partyRepository.increaseHit(partyId);
    }

    public Map<String, Long> createParty(User user, PartyCreateDto request) {
        log.info("=== createParty() start ===");

        Long userId = user.getId();
        User findUser = userRepository.findById(userId).orElseThrow(() -> new UserException(UserExceptionType.NOT_FOUND_USER));

        checkParticipant(request.getTotalParticipant());

        // address 변환, thumbnail이 null일 경우 처리하는 로직 처리 후 생성
        Party party = createBasePartyBuilder(request, findUser);
        Party savedParty = partyRepository.save(party);

        Team team = Team.builder()
                .user(findUser)
                .party(savedParty)
                .role(Role.HOST)
                .build();

        teamRepository.save(team);

        Map<String, Long> partyId = new HashMap<>();
        partyId.put("partyId", savedParty.getId());

        eventPublisher.publishEvent(new CreateRoomEvent(savedParty.getId(), user.getId()));

        return partyId;
    }

    private Point2D.Double setLocationFunc(double latitude, double longitude) {
        Point2D.Double now = new Point2D.Double();
        now.setLocation(latitude, longitude);
        return now;
    }

    private void checkParticipant(int totalParticipant) {
        if (totalParticipant < 2) {
            throw new PartyException(PartyExceptionType.NOT_MINIMUM_PARTICIPANT);
        }
    }

    private String getThumbnail(PartyCategory category, String thumbnail) {

        if (thumbnail == null) {
            switch (category) {
                case KOREAN -> thumbnail = url + "korean.jpeg";
                case WESTERN -> thumbnail = url + "western.jpeg";
                case CHINESE -> thumbnail = url + "chinese.jpeg";
                case JAPANESE -> thumbnail = url + "japanese.jpeg";
                case ETC -> thumbnail = url + "etc.jpeg";
            }
        }
        return thumbnail;
    }

    private LocalDateTime getDeadline(LocalDateTime deadline, LocalDateTime partyTime) {
        if (deadline == null) {
            deadline = partyTime.minusHours(1L);
        }
        return deadline;
    }

    private String getAddress(double longitude, double latitude) {
        String address = mapService.coordToAddr(longitude, latitude);
        if (address.equals("")) {
            throw new MapException(MapExceptionType.NOT_FOUND_ADDRESS);
        }
        return address;
    }

    public void partyUpdate(PartyUpdateDto partyUpdateDto, Long partyId) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new PartyException(PartyExceptionType.NOT_FOUND_PARTY));
        if (partyUpdateDto.partyTitle() != null) {
            party.setPartyTitle(partyUpdateDto.partyTitle());
        }
        if (partyUpdateDto.partyContent() != null) {
            party.setPartyContent(partyUpdateDto.partyContent());
        }
        if (partyUpdateDto.menu() != null) {
            party.setMenu(partyUpdateDto.menu());
        }
        if (partyUpdateDto.longitude() != null && partyUpdateDto.latitude() != null) {
            party.setLatitude(partyUpdateDto.latitude());
            party.setLongitude(partyUpdateDto.longitude());
            String address = mapService.coordToAddr(partyUpdateDto.longitude(), partyUpdateDto.latitude());
            party.setAddress(address);
        }
        if (partyUpdateDto.status() != null) {
            party.setStatus(partyUpdateDto.status());
        }
        if (partyUpdateDto.thumbnail() != null) {
            party.setThumbnail(partyUpdateDto.thumbnail());
        }
        if (partyUpdateDto.partyTime() != null) {
            party.setPartyTime(partyUpdateDto.partyTime());
            party.setDeadline(partyUpdateDto.partyTime().minusHours(1));
        }
        if (partyUpdateDto.totalParticipant() != null) {
            if (party.getParticipantCount() <= partyUpdateDto.totalParticipant()) {
                party.setTotalParticipant(partyUpdateDto.totalParticipant());
            } else {
                throw new PartyException(PartyExceptionType.INVALID_UPDATE_VALUE);
            }
        }
        if (partyUpdateDto.gender() != null) {
            party.setGender(partyUpdateDto.gender());
        }
        if (partyUpdateDto.age() != null) {
            party.setAge(partyUpdateDto.age());
        }
    }

    private boolean timeValidCheck(LocalDateTime deadlineTime, LocalDateTime partyTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(deadlineTime) && deadlineTime.isBefore(partyTime)) {
            return true;
        } else {
            throw new PartyException(PartyExceptionType.INVALID_UPDATE_VALUE);
        }
    }

    public void deleteParty(User user, Long partyId) {
        Party party = partyRepository.findById(partyId).orElseThrow(() -> new PartyException(PartyExceptionType.NOT_FOUND_PARTY));
        if (!user.getId().equals(party.getUser().getId())) {
            throw new UserException(UserExceptionType.NOT_MATCH_USER);
        }
        partyRepository.delete(party);
    }

    // address, deadline, thumbnail와 같이 변환이나 null인 경우 처리가 필요한 필드는 제외하고 나머지 필드는 빌더패턴으로 생성
    private Party createBasePartyBuilder(PartyCreateDto request, User user) {
        return Party.builder()
                .partyTitle(request.getTitle())
                .partyContent(request.getContent())
                .latitude(setLocationFunc(request.getLatitude(), request.getLongitude()).x)
                .longitude(setLocationFunc(request.getLatitude(), request.getLongitude()).y)
                .partyTime(request.getPartyTime())
                .totalParticipant(request.getTotalParticipant())
                .gender(request.getGender())
                .age(request.getAge())
                .menu(request.getMenu())
                .category(request.getCategory())
                .status(PartyStatus.RECRUIT)
                .user(user)
                .address(getAddress(request.getLongitude(), request.getLatitude()))
                .deadline(request.getPartyTime().minusHours(1))
                .thumbnail(getThumbnail(request.getCategory(), request.getThumbnail()))
                .build();
    }

    public Long joinParty(PartyJoinDto partyJoinDto, User user) {
        log.info("=== joinParty() start ===");

        Party party = partyRepository.findById(partyJoinDto.partyId()).orElseThrow(() -> new PartyJoinException(PartyJoinExceptionType.NOT_FOUND_PARTY_JOIN));
        PartyJoin partyJoin = PartyJoin.builder().party(party).leaderId(party.getUser().getId()).userId(user.getId()).build();
        Optional<PartyJoin> byPartyIdAndLeaderIdAndUserId = partyJoinRepository.findByPartyIdAndLeaderIdAndUserId(partyJoin.getParty().getId(), partyJoin.getLeaderId(), partyJoin.getUserId());

        if (!party.getUser().getId().equals(party.getUser().getId())) {
            throw new UserException(UserExceptionType.NOT_FOUND_USER);
        }

        if (partyJoinDto.status() == PartyJoinStatus.APPLY) {
            if (!byPartyIdAndLeaderIdAndUserId.isEmpty()) {
                throw new PartyJoinException(PartyJoinExceptionType.DUPLICATION_PARTY_JOIN);
            }
            PartyJoin savedpartyJoin = partyJoinRepository.save(partyJoin);
            return savedpartyJoin.getId();
        }else if (partyJoinDto.status() == PartyJoinStatus.CANCEL) {
            if (byPartyIdAndLeaderIdAndUserId.isEmpty()) {
                throw new PartyJoinException(PartyJoinExceptionType.NOT_FOUND_PARTY_JOIN);
            }
            partyJoinRepository.delete(byPartyIdAndLeaderIdAndUserId.get());
            return byPartyIdAndLeaderIdAndUserId.get().getId();
        }
        return null;
    }

    public String decideUser(PartyDecisionDto partyDecisionDto, User user) {
        log.info("=== decideUser() start ===");

        if (!(partyDecisionDto.getStatus() == PartyDecision.ACCEPT || partyDecisionDto.getStatus() == PartyDecision.REFUSE)) {
            log.error("=== Party Join Status was requested incorrectly ===");
            throw new PartyJoinException(PartyJoinExceptionType.WRONG_STATUS);
        }

        User volunteerUser = userRepository.findByNickname(partyDecisionDto.getNickname()).orElseThrow(() -> new UserException(UserExceptionType.NOT_FOUND_USER));
        PartyJoin findPartyJoin = partyJoinRepository.findByPartyIdAndUserId(
                partyDecisionDto.getPartyId(),
                volunteerUser.getId()).orElseThrow(() -> new PartyJoinException(PartyJoinExceptionType.NOT_FOUND_PARTY_JOIN));
        partyJoinRepository.delete(findPartyJoin);

        if (partyDecisionDto.getStatus() == PartyDecision.ACCEPT) {
            log.info("=== ACCEPT ===");

            Party party = partyRepository.findById(partyDecisionDto.getPartyId()).orElseThrow(() -> new PartyException(PartyExceptionType.NOT_FOUND_PARTY));
            party.increaseUser();

            if (party.getTotalParticipant() == party.getParticipantCount()) {
                party.setStatus(PartyStatus.RECRUIT_FINISH);
            }

            Team member = Team.builder().user(volunteerUser).party(party).role(Role.VOLUNTEER).build();
            teamRepository.save(member);
            return "Accept Request Completed";
        } else{
            log.info("=== REFUSE ===");
            return "Refuse Request Completed";
        }
    }

    public List<InvitationRequestDto> getJoinList(User user, Role role) {
        List<PartyJoin> partyJoinList;
        List<InvitationRequestDto> invitationRequestDtos;

        if (role.equals(Role.HOST)) {
            partyJoinList = partyJoinRepository.findAllByLeaderId(user.getId());
            invitationRequestDtos = partyJoinList.stream().map(partyJoin -> {
                User volunteerUser = userRepository.findById(partyJoin.getUserId()).orElseThrow(() -> new UserException(UserExceptionType.NOT_FOUND_USER));
                return InvitationRequestDto.toDto(partyJoin, volunteerUser, role);
            }).toList();
        } else if (role.equals(Role.VOLUNTEER)) {
            partyJoinList = partyJoinRepository.findAllByUserId(user.getId());
            invitationRequestDtos = partyJoinList.stream().map(partyJoin -> {
                User leaderUser = userRepository.findById(partyJoin.getLeaderId()).orElseThrow(() -> new UserException(UserExceptionType.NOT_FOUND_USER));
                return InvitationRequestDto.toDto(partyJoin, leaderUser, role);
            }).toList();
        } else {
            throw new UserException(UserExceptionType.INVALID_ROLE_USER);
        }
        return invitationRequestDtos;
    }
}
