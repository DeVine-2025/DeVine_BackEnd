package com.umc.devine.admin.auth.service;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "누가 관리자인가"를 판정하는 서비스.
 *
 * <p>판정은 오직 {@code clerk_id}(JWT의 sub, 위조 불가)만 신뢰한다. 이메일/이메일 검증 같은
 * Clerk 클레임 의미론에 권한 경계를 의존시키지 않는다.
 *
 * <p>판정 순서:
 * <ol>
 *   <li>admin 테이블에 (활성 여부 무관) 존재하면 그 행의 활성 상태를 따른다.
 *       비활성(회수)이면 관리자가 아니다 — 부트스트랩 목록에 남아 있어도 회수를 우선한다.</li>
 *   <li>없지만 clerk_id가 부트스트랩 목록에 있으면, admin 테이블에 자동 등록(lazy seeding) 후 사용</li>
 *   <li>둘 다 아니면 관리자가 아님(빈 결과)</li>
 * </ol>
 *
 * <p>이 로직을 시큐리티 계층에서 분리해, converter가 트랜잭션 경계 안에서 이 서비스에만 위임하도록 한다.
 */
@Slf4j
@Service
public class AdminAuthorizationService {

    private static final String BOOTSTRAP_GRANTED_BY = "BOOTSTRAP";

    private final AdminRepository adminRepository;
    private final Set<String> bootstrapClerkIds;

    public AdminAuthorizationService(AdminRepository adminRepository,
                                     @Value("${admin.bootstrap-clerk-ids:}") String bootstrapClerkIdsRaw) {
        this.adminRepository = adminRepository;
        this.bootstrapClerkIds = parseClerkIds(bootstrapClerkIdsRaw);
    }

    /**
     * clerkId로 관리자를 판정한다. 부트스트랩 clerk_id면 최초 접근 시 자동 등록한다.
     *
     * @param email JWT의 email 클레임(있으면 표시/감사용으로 저장, 없으면 null). 판정에는 쓰이지 않는다.
     * @return 활성 관리자면 해당 Admin, 아니면 빈 Optional
     */
    @Transactional
    public Optional<Admin> resolveAdmin(String clerkId, String email) {
        // 회수 우선: clerk_id 행이 있으면(활성 여부 무관) 그 활성 상태를 따른다.
        Optional<Admin> byClerkId = adminRepository.findByClerkId(clerkId);
        if (byClerkId.isPresent()) {
            return activeOrEmpty(byClerkId.get());
        }

        // 부트스트랩은 clerk_id(sub)만 신뢰한다.
        if (clerkId == null || !bootstrapClerkIds.contains(clerkId)) {
            return Optional.empty();
        }

        return seedBootstrapAdmin(clerkId, email);
    }

    private Optional<Admin> seedBootstrapAdmin(String clerkId, String email) {
        // ON CONFLICT DO NOTHING: 동시 최초 접근/재진입에도 예외 없이 멱등하게 처리(트랜잭션 abort 차단).
        adminRepository.insertBootstrapAdminIfAbsent(clerkId, normalizeEmail(email), BOOTSTRAP_GRANTED_BY);

        // DO NOTHING은 행을 반환하지 않으므로 후속 SELECT로 확인한다.
        Optional<Admin> seeded = adminRepository.findByClerkId(clerkId);
        seeded.ifPresent(a -> log.info("[ADMIN] bootstrap 관리자 등록/확인 clerkId={}", clerkId));
        return seeded.flatMap(this::activeOrEmpty);
    }

    private Optional<Admin> activeOrEmpty(Admin admin) {
        return admin.isActive() ? Optional.of(admin) : Optional.empty();
    }

    private Set<String> parseClerkIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(java.util.Locale.ROOT) : null;
    }
}