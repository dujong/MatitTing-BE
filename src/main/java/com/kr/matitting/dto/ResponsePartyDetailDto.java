package com.kr.matitting.dto;

import com.kr.matitting.constant.Gender;
import com.kr.matitting.constant.PartyAge;
import com.kr.matitting.constant.PartyCategory;
import com.kr.matitting.constant.PartyStatus;
import com.kr.matitting.entity.Party;
import com.kr.matitting.entity.Review;
import com.kr.matitting.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePartyDetailDto {
    @Schema(description = "파티 방장 id", example = "1")
    private Long hostId;

    @Schema(description = "방장 여부", example = "true or false")
    private Boolean isLeader;
    @Schema(description = "파티 id", nullable = false, example = "1")
    @NotNull
    private Long partyId;

    @Schema(description = "파티 제목", nullable = false, example = "붕어빵 드실 분")
    @NotNull
    private String partyTitle;
    @Schema(description = "파티 내용", nullable = false, example = "붕어빵은 팥이 근본입니다.")
    @NotNull
    private String partyContent;
    @Schema(description = "주소", nullable = false, example = "서울 송파구 송파동 7-1")
    @NotNull
    private String address;
    @Schema(description = "경도", nullable = false, example = "126.88453591058602")
    @NotNull
    private double longitude;
    @Schema(description = "위도", nullable = false, example = "37.53645109566274")
    @NotNull
    private double latitude;
    @Schema(description = "파티 장소", nullable = true, example = "달달 블라썸")
    private String partyPlaceName;
    @Schema(description = "파티 상태", nullable = false, example = "RECRUIT")
    @NotNull
    private PartyStatus status;
    @Schema(description = "성별", nullable = false, example = "ALL")
    @NotNull
    private Gender gender;
    @Schema(description = "연령대", nullable = false, example = "TWENTY")
    @NotNull
    private PartyAge age;
    @Schema(description = "파티 모집 마감 시간", nullable = false, example = "2023-10-24T09:00:00")
    @NotNull
    private LocalDateTime deadline;
    @Schema(description = "파티 시작 시간", nullable = false, example = "2023-10-24T10:00:00")
    @NotNull
    private LocalDateTime partyTime;
    @Schema(description = "모집 인원", nullable = false, example = "4")
    @NotNull
    private Integer totalParticipant;
    @Schema(description = "현재 참가 인원", nullable = false, example = "2")
    @NotNull
    private Integer participate;
    @Schema(description = "파티 메뉴", nullable = true, example = "붕어빵")
    private String menu;
    @Schema(description = "카테고리", nullable = false, example = "KOREAN")
    private PartyCategory category;
    @Schema(description = "썸네일", nullable = true, example = " https://matitting.s3.ap-northeast-2.amazonaws.com/korean.jpeg")
    @NotNull
    private String thumbnail;
    @Schema(description = "조회수", nullable = false, example = "1")
    @NotNull
    private Integer hit;

    private List<ReviewInfoRes> reviewInfoRes = new ArrayList<>();
    public static ResponsePartyDetailDto from(Party party, User user) {
        User host = party.getUser();
        List<Review> reviews = host.getReceivedReviews().stream().sorted(Comparator.comparing(Review::getCreateDate).reversed()).toList().subList(0, min(3, host.getReceivedReviews().size()));
        List<ReviewInfoRes> reviewInfoResStream = reviews.stream().map(review -> ReviewInfoRes.toDto(review, host)).toList();
        return ResponsePartyDetailDto.builder()
                .hostId(host.getId())
                .isLeader(user != null && user.getId().equals(host.getId()))
                .partyId(party.getId())
                .partyTitle(party.getPartyTitle())
                .partyContent(party.getPartyContent())
                .address(party.getAddress())
                .longitude(party.getLongitude())
                .latitude(party.getLatitude())
                .partyPlaceName(party.getPartyPlaceName())
                .status(party.getStatus())
                .gender(party.getGender())
                .age(party.getAge())
                .deadline(party.getDeadline())
                .partyTime(party.getPartyTime())
                .totalParticipant(party.getTotalParticipant())
                .participate(party.getParticipantCount())
                .menu(party.getMenu())
                .category(party.getCategory())
                .thumbnail(party.getThumbnail())
                .hit(party.getHit())
                .reviewInfoRes(reviewInfoResStream)
                .build();
    }
}
