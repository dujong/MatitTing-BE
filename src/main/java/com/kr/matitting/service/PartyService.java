package com.kr.matitting.service;

import com.kr.matitting.constant.*;
import com.kr.matitting.dto.*;
import com.kr.matitting.entity.*;
import com.kr.matitting.exception.Map.MapException;
import com.kr.matitting.exception.Map.MapExceptionType;
import com.kr.matitting.exception.party.PartyException;
import com.kr.matitting.exception.party.PartyExceptionType;
import com.kr.matitting.exception.partyjoin.PartyJoinException;
import com.kr.matitting.exception.partyjoin.PartyJoinExceptionType;
import com.kr.matitting.exception.user.UserException;
import com.kr.matitting.exception.user.UserExceptionType;
import com.kr.matitting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.geom.Point2D;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PartyService {
    @Value("${cloud.aws.s3.url}")
    private String url;
    private final PartyJoinRepository partyJoinRepository;
    private final PartyTeamRepository teamRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final MapService mapService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final PartyJoinRepositoryCustom partyJoinRepositoryCustom;
    private final EntityFacade entityFacade;

    public ResponsePartyDetailDto getPartyInfo(Long userId, Long partyId) {
        Optional<User> optUser = entityFacade.getOptUser(userId);
        Party party = entityFacade.getParty(partyId);
        increaseHit(partyId);

        return ResponsePartyDetailDto.from(party, optUser.get());
    }

    public void increaseHit(Long partyId) {
        partyRepository.increaseHit(partyId);
    }

    public ResponseCreatePartyDto createParty(Long userId, PartyCreateDto request) {
        log.info("=== createParty() start ===");

        User user = entityFacade.getUser(userId);
        // address 변환, deadline, thumbnail이 null일 경우 처리하는 로직 처리 후 생성
        Party party = createBasePartyBuilder(request, user);
        Party savedParty = partyRepository.save(party);

        Team team = new Team(user, savedParty, Role.HOST);
        teamRepository.save(team);

        // 채팅방 생성
        ChatRoom createdChatRoom = chatService.createChatRoom(party, user);

        return new ResponseCreatePartyDto(savedParty, createdChatRoom);
    }

    private Point2D.Double setLocationFunc(double latitude, double longitude) {
        Point2D.Double now = new Point2D.Double();
        now.setLocation(latitude, longitude);
        return now;
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

    public void partyUpdate(Long userId, PartyUpdateDto partyUpdateDto, Long partyId) {
        User user = entityFacade.getUser(userId);
        Party party = entityFacade.getParty(partyId);

        checkRole(user.getId(), party);
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
        if (partyUpdateDto.partyPlaceName() != null) {
            party.setPartyPlaceName(partyUpdateDto.partyPlaceName());
        }
        if (partyUpdateDto.status() != null) {
            if (partyUpdateDto.status().equals(PartyStatus.RECRUIT_FINISH)){
                party.getTeamList()
                        .stream()
                        .map(Team::getUser)
                        .filter(teamUser -> teamUser.getId() != user.getId())
                        .forEach(teamUser -> notificationService.send(teamUser, party.getUser(), party, NotificationType.RECRUIT_FINISH, "파티 모집 완료", party.getPartyTitle() + " 파티 모집이 완료되었습니다."));
            }
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

    public void deleteParty(Long userId, Long partyId) {
        User user = entityFacade.getUser(userId);
        Party party = entityFacade.getParty(partyId);

        checkRole(user.getId(), party);
        partyRepository.delete(party);
    }

    private void checkRole(Long userId, Party party) {
        if (!userId.equals(party.getUser().getId())) {
            throw new UserException(UserExceptionType.INVALID_ROLE_USER);
        }
    }

    // address, deadline, thumbnail와 같이 변환이나 null인 경우 처리가 필요한 필드는 제외하고 나머지 필드는 빌더패턴으로 생성
    private Party createBasePartyBuilder(PartyCreateDto request, User user) {
        return Party.builder()
                .partyTitle(request.getPartyTitle())
                .partyContent(request.getPartyContent())
                .partyPlaceName(request.getPartyPlaceName())
                .latitude(setLocationFunc(request.getLatitude(), request.getLongitude()).x)
                .longitude(setLocationFunc(request.getLatitude(), request.getLongitude()).y)
                .partyTime(request.getPartyTime())
                .totalParticipant(request.getTotalParticipant())
                .participantCount(1)
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

    public ResponseCreatePartyJoinDto joinParty(PartyJoinDto partyJoinDto, Long userId) {
        log.info("=== joinParty() start ===");

        User user = entityFacade.getUser(userId);
        Party party = entityFacade.getParty(partyJoinDto.partyId());

        if (party.getUser().getId().equals(user.getId())) throw new UserException(UserExceptionType.INVALID_ROLE_USER);

        Optional<PartyJoin> existingJoin = partyJoinRepository.findByPartyIdAndLeaderIdAndUserId(party.getId(), party.getUser().getId(), user.getId());

        switch (partyJoinDto.status()) {
            case APPLY:
                if (existingJoin.isPresent())
                    throw new PartyJoinException(PartyJoinExceptionType.DUPLICATION_PARTY_JOIN);

                PartyJoin savedpartyJoin = partyJoinRepository.save(new PartyJoin(party, party.getUser().getId(), user.getId(), partyJoinDto.oneLineIntroduce()));
                notificationService.send(party.getUser(), user, party, NotificationType.PARTICIPATION_REQUEST, "파티 신청", party.getPartyTitle() + " 파티에 참가 신청이 도착했어요.");

                return new ResponseCreatePartyJoinDto(savedpartyJoin.getId());

            case CANCEL:
                if (existingJoin.isEmpty())
                    throw new PartyJoinException(PartyJoinExceptionType.NOT_FOUND_PARTY_JOIN);

                partyJoinRepository.delete(existingJoin.get());
                return new ResponseCreatePartyJoinDto(existingJoin.get().getId());

            default:
                return null;
        }
    }

    public String decideUser(PartyDecisionDto partyDecisionDto, Long userId) {
        log.info("=== decideUser() start ===");

        User user = entityFacade.getUser(userId);
        User volunteerUser = userRepository.findByNickname(partyDecisionDto.getNickname()).orElseThrow(() -> new UserException(UserExceptionType.NOT_FOUND_USER));
        PartyJoin findPartyJoin = partyJoinRepository.findByPartyIdAndUserId(
                partyDecisionDto.getPartyId(),
                volunteerUser.getId()).orElseThrow(() -> new PartyJoinException(PartyJoinExceptionType.NOT_FOUND_PARTY_JOIN));

        if (!findPartyJoin.getLeaderId().equals(user.getId()))
            throw new UserException(UserExceptionType.INVALID_ROLE_USER);

        partyJoinRepository.delete(findPartyJoin);

        Party party = entityFacade.getParty(partyDecisionDto.getPartyId());
        if (partyDecisionDto.getStatus() == PartyDecision.ACCEPT) {
            log.info("=== ACCEPT ===");

            party.increaseUser();
            Team member = new Team(volunteerUser, party, Role.VOLUNTEER);
            teamRepository.save(member);

            //참가 수락된 유저를 채팅방에 추가
            chatService.addParticipant(party, volunteerUser);

            notificationService.send(volunteerUser, party.getUser(), party, NotificationType.REQUEST_DECISION, "참가신청 여부", party.getPartyTitle() + "파티에 참가 되셨습니다.");
            return "Accept Request Completed";
        } else {
            log.info("=== REFUSE ===");
            notificationService.send(volunteerUser, party.getUser(), party, NotificationType.REQUEST_DECISION, "참가신청 여부", party.getPartyTitle() + "파티에 참가가 거절되었습니다.");
            return "Refuse Request Completed";
        }
    }

    public ResponseGetPartyJoinDto getJoinList(Long userId, Role role, Pageable pageable) {
        User user = entityFacade.getUser(userId);
        Page<PartyJoin> partyJoinPage = partyJoinRepositoryCustom.getPartyJoin(pageable, user, role);

        switch (role) {
            case HOST:
                List<InvitationRequestDto> hostInvitation = partyJoinPage.getContent().stream()
                        .map(partyJoin -> {
                            User volunteer = entityFacade.getUser(partyJoin.getUserId());
                            return InvitationRequestDto.toDto(partyJoin, volunteer, role);
                        })
                        .toList();
                ResponsePageInfoDto hostPageInfoDto = new ResponsePageInfoDto(partyJoinPage.getPageable().getPageNumber(), partyJoinPage.hasNext());
                return new ResponseGetPartyJoinDto(hostInvitation, hostPageInfoDto);
            case VOLUNTEER:
                List<InvitationRequestDto> volunteerInvitation = partyJoinPage.getContent().stream()
                        .map(partyJoin -> {
                            User leader = entityFacade.getUser(partyJoin.getLeaderId());
                            return InvitationRequestDto.toDto(partyJoin, leader, role);
                        })
                        .toList();
                ResponsePageInfoDto volunteerPageInfoDto = new ResponsePageInfoDto(partyJoinPage.getPageable().getPageNumber(), partyJoinPage.hasNext());
                return new ResponseGetPartyJoinDto(volunteerInvitation, volunteerPageInfoDto);
            default:
                throw new UserException(UserExceptionType.INVALID_ROLE_USER);
        }
    }
}
