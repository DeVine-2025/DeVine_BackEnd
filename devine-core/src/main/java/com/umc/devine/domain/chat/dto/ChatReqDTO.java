package com.umc.devine.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatReqDTO {

    public record CreateRoomReq(
            @NotNull(message = "대상 회원 ID는 필수입니다.")
            Long targetMemberId
    ) {}

    public record SendMessageReq(
            @NotNull(message = "메시지 내용은 필수입니다.")
            @Size(min = 1, max = 1000, message = "메시지는 1~1000자여야 합니다.")
            String content
    ) {}
}
