package com.umc.devine.global.validation.validator;

import com.umc.devine.global.validation.annotation.ValidNickname;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NicknameValidator implements ConstraintValidator<ValidNickname, String> {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;

    @Override
    public boolean isValid(String nickname, ConstraintValidatorContext context) {
        if (nickname == null || nickname.isBlank()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("닉네임은 필수입니다.")
                    .addConstraintViolation();
            return false;
        }

        int length = nickname.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return false;
        }

        return true;
    }
}
