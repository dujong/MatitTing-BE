package com.kr.matitting.dto;

import com.kr.matitting.constant.PartyCategory;
import com.kr.matitting.constant.PartyGender;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreatePartyRequest {

    private String title;
    private String content;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime partyTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;

    private int totalParticipant;
    private String longitude;
    private String latitude;
    private PartyGender gender;
    private PartyCategory category;
    private String menu;
    private String thumbnail;

}