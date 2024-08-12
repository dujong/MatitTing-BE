package com.kr.matitting.service;

import com.kr.matitting.entity.*;
import com.kr.matitting.exception.chat.ChatException;
import com.kr.matitting.exception.chat.ChatExceptionType;
import com.kr.matitting.exception.party.PartyException;
import com.kr.matitting.exception.party.PartyExceptionType;
import com.kr.matitting.exception.partyjoin.PartyJoinException;
import com.kr.matitting.exception.partyjoin.PartyJoinExceptionType;
import com.kr.matitting.exception.reivew.ReviewException;
import com.kr.matitting.exception.reivew.ReviewExceptionType;
import com.kr.matitting.exception.team.TeamException;
import com.kr.matitting.exception.team.TeamExceptionType;
import com.kr.matitting.exception.user.UserException;
import com.kr.matitting.exception.user.UserExceptionType;
import com.kr.matitting.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class EntityFacade {
    private final UserRepository userRepository;
    private final PartyRepository partyRepository;
    private final PartyJoinRepository partyJoinRepository;
    private final PartyTeamRepository teamRepository;
    private final ReviewRepository reviewRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatUserRepository chatUserRepository;

    public User getUser(Long userId) {
        Optional<User> userById = userRepository.findById(userId);
        if (userById.isEmpty()) throw new UserException(UserExceptionType.NOT_FOUND_USER);
        return userById.get();
    }

    public Optional<User> getOptUser(Long userId) {
        return userRepository.findById(userId);
    }

    public Party getParty(Long partyId) {
        Optional<Party> partyById = partyRepository.findById(partyId);
        if (partyById.isEmpty()) throw new PartyException(PartyExceptionType.NOT_FOUND_PARTY);
        return partyById.get();
    }

    public PartyJoin getPartyJoin(Long partyJoinId) {
        Optional<PartyJoin> partyJoinById = partyJoinRepository.findById(partyJoinId);
        if (partyJoinById.isEmpty()) throw new PartyJoinException(PartyJoinExceptionType.NOT_FOUND_PARTY_JOIN);
        return partyJoinById.get();
    }

    public Team getTeam(Long teamId) {
        Optional<Team> teamById = teamRepository.findById(teamId);
        if (teamById.isEmpty()) throw new TeamException(TeamExceptionType.NOT_FOUND_TEAM);
        return teamById.get();
    }

    public Review getReview(Long reviewId) {
        Optional<Review> reviewById = reviewRepository.findById(reviewId);
        if (reviewById.isEmpty()) throw new ReviewException(ReviewExceptionType.NOT_FOUND_REVIEW);
        return reviewById.get();
    }

    public ChatRoom getChatRoom(Long chatRoomId) {
        Optional<ChatRoom> chatRoomById = chatRoomRepository.findById(chatRoomId);
        if (chatRoomById.isEmpty()) throw new ChatException(ChatExceptionType.NOT_FOUND_CHAT_ROOM);
        return chatRoomById.get();
    }

    public ChatUser getChatUser(Long chatUserId) {
        Optional<ChatUser> chatUserById = chatUserRepository.findById(chatUserId);
        if (chatUserById.isEmpty()) throw new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER_INFO);
        return chatUserById.get();
    }
    public ChatUser getChatUserByUserIdAndChatRoomId(Long userId, Long roomId) {
        Optional<ChatUser> chatUserByUserIdAndChatRoomId = chatUserRepository.findByUserIdAndChatRoomId(userId, roomId);
        if (chatUserByUserIdAndChatRoomId.isEmpty())
            throw new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER_INFO);
        return chatUserByUserIdAndChatRoomId.get();
    }

    public ChatUser getChatUserByChatUserId(Long userId) {
        Optional<ChatUser> chatUserByUserIdAndChatRoomId = chatUserRepository.findById(userId);
        if (chatUserByUserIdAndChatRoomId.isEmpty())
            throw new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER_INFO);
        return chatUserByUserIdAndChatRoomId.get();
    }

    public List<ChatUser> getChatUsersByUserId(Long userId) {
        List<ChatUser> ChatUsersByUserId = chatUserRepository.findByUserId(userId);
        if (ChatUsersByUserId.isEmpty()) throw new ChatException(ChatExceptionType.IS_NOT_HAVE_CHAT_ROOM);
        return ChatUsersByUserId;
    }

    public List<ChatUser> getChatUsersByRoomId(Long roomId) {
        List<ChatUser> chatUsersByRoomId = chatUserRepository.findByChatRoomId(roomId);
        if (chatUsersByRoomId.isEmpty()) throw new ChatException(ChatExceptionType.NOT_FOUND_CHAT_USER_INFO);
        return chatUsersByRoomId;
    }
}
