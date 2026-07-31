package com.umc.devine.admin.enums;

/**
 * 관리자 권한 레벨.
 * 현재는 단일 ADMIN만 사용하며, 다단계 권한이 필요해지면 이 enum과
 * admin_level_check 제약(마이그레이션)을 함께 확장한다.
 */
public enum AdminLevel {
    ADMIN
}