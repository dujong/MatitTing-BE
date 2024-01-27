package com.kr.matitting.oauth2.dto;

import com.kr.matitting.constant.OauthProvider;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
public class KakaoParams implements OauthParams{
    private String authorizationCode;

    @Override
    public OauthProvider oauthProvider() {
        return OauthProvider.KAKAO;
    }

    @Override
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    @Override
    public MultiValueMap<String, String> makeBody() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode);
        return body;
    }
}
