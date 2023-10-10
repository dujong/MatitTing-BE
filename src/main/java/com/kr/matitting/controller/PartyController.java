package com.kr.matitting.controller;

import com.kr.matitting.dto.CreatePartyRequest;
import com.kr.matitting.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.kr.matitting.dto.PartyJoinDto;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
public class PartyController {
    private final PartyService partyService;
    @PostMapping("/party")
    public void createParty(
            @RequestBody CreatePartyRequest request
    ) {
        partyService.createParty(request);
    }

    //유저가 파티방에 참가를 요청하는 logic
    @PostMapping("/api/party/{partyId}/{참가Id}")
    public ResponseEntity JoinParty(PartyJoinDto partyJoinDto) throws ChangeSetPersister.NotFoundException {
        partyService.joinParty(partyJoinDto);
        return ResponseEntity.ok().body("Success join request!");
    }

    //방장이 파티방에 대한 수락/거절을 하는 logic
    @PostMapping("/api/party/{partyId}/{방장Id}/{참가Id}")
    public ResponseEntity AcceptRefuseParty(PartyJoinDto partyJoinDto) throws ChangeSetPersister.NotFoundException {
        String result = partyService.decideUser(partyJoinDto);
        return ResponseEntity.ok().body(result);
    }

}
