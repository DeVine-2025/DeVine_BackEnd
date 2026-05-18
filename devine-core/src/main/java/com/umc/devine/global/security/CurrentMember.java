package com.umc.devine.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 현재 인증된 Member 엔티티를 주입합니다.
 *
 * required=false인 경우 비인증 요청에서는 null이 주입됩니다 (permitAll 엔드포인트에서 옵셔널 인증 처리용).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
    boolean required() default true;
}
