package com.umc.devine.admin.repository;

import com.umc.devine.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    // 활성 여부와 무관하게 존재를 확인한다. 회수(비활성)된 관리자도 조회되어야
    // 부트스트랩 재등록을 막고 UNIQUE 제약 위반(→ 트랜잭션 abort → 500)을 피할 수 있다.
    Optional<Admin> findByClerkId(String clerkId);

    /**
     * 부트스트랩 관리자를 멱등하게 삽입한다.
     *
     * <p>대상 없는 {@code ON CONFLICT DO NOTHING}이라 clerk_id(또는 email) UNIQUE에 충돌해도
     * 예외 없이 no-op된다. 덕분에 @Transactional 안에서 제약 위반으로 트랜잭션이 abort되는 문제를
     * 원천 차단한다. 단 DO NOTHING은 행을 반환하지 않으므로 호출부에서 후속 SELECT가 필요하다.
     *
     * <p>email은 nullable이며 판정에는 쓰이지 않는다(표시/감사용).
     * level / is_active / created_at은 테이블 기본값을 사용한다.
     * created_by는 네이티브 INSERT라 JPA 감사(auditing)를 우회해 null이며, 출처는 granted_by로 남긴다.
     */
    @Modifying
    @Query(value = "INSERT INTO admin (clerk_id, email, granted_by) "
            + "VALUES (:clerkId, :email, :grantedBy) "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertBootstrapAdminIfAbsent(@Param("clerkId") String clerkId,
                                      @Param("email") String email,
                                      @Param("grantedBy") String grantedBy);
}