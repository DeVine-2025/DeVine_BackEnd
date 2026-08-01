package com.umc.devine.infrastructure.email;

import lombok.Builder;
import lombok.Getter;

// 이메일 발송이 호출자의 DB 트랜잭션에 묶이지 않도록 이벤트로 분리한다.
@Getter
@Builder
public class EmailNotificationEvent {

    private final String to;
    private final String subject;
    private final String body;
}
