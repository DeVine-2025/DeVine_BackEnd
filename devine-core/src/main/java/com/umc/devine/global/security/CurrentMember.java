package com.umc.devine.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 현재 인증된 Member 엔티티를 주입합니다.
 *
 * required=false인 경우 비인증 요청에서는 null이 주입됩니다 (permitAll 엔드포인트에서 옵셔널 인증 처리용).
 *
 * includeSanctioned=true인 경우 정지(SUSPENDED)/강제탈퇴 예정(PENDING_WITHDRAWAL) 회원도 주입합니다.
 * 이용약관 제7조에 따라 제재 중에도 탈퇴는 할 수 있어야 하므로 회원 탈퇴 API에서만 사용합니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
    boolean required() default true;

    boolean includeSanctioned() default false;
}
