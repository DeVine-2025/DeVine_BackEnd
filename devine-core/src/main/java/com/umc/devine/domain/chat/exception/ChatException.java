package com.umc.devine.domain.chat.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class ChatException extends DomainException {
    public ChatException(DomainErrorReason reason) {
        super(reason);
    }
}
