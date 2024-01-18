package com.kr.matitting.entity;

import com.kr.matitting.constant.Gender;
import com.kr.matitting.constant.PartyAge;
import com.kr.matitting.constant.PartyCategory;
import com.kr.matitting.constant.PartyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(name = "PARTY")
public class Party extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    @Column(nullable = false, name = "title")
    private String partyTitle; // 파티 모집 제목

    @Column(nullable = false, name = "content", length = 500)
    private String partyContent; // 파티 모집 글

    @Column(nullable = false, name = "address")
    private String address; // 주소

    @Column(name = "longitude")
    private double longitude;

    @Column(name = "latitude")
    private double latitude;

    @Enumerated(EnumType.STRING)
    private PartyStatus status; // 파티 상태

    @Column(name = "deadline")
    private LocalDateTime deadline; // 파티 모집 시간

    @Column(nullable = false, name = "party_time")
    private LocalDateTime partyTime; // 파티 시작 시간

    @Column(nullable = false, name = "total_participant")
    private int totalParticipant; // 모집 인원

    @Column(name = "participant_count")
    @ColumnDefault("1")
    private int participantCount; // 참가자 수

    @Column(nullable = false, name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender; // 참여 가능 성별

    @Column(nullable = false, name = "age")
    @Enumerated(EnumType.STRING)
    private PartyAge age; // 연령대

    @Column(name = "hit")
    @ColumnDefault("0")
    private int hit;

    @Column(name = "thumbnail")
    private String thumbnail;

    @Column(name = "menu")
    private String menu;

    @Column(name = "category")
    private PartyCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teamList = new ArrayList<>();

    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyJoin> partyJoinList = new ArrayList<>();

    @OneToOne(mappedBy = "party", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ChatRoom chatRoom;

    public void increaseUser() {
        this.participantCount++;
    }

}