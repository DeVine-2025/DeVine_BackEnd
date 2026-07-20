package com.umc.devine.admin.auth.service;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "누가 관리자인가"를 판정하는 서비스.
 *
 * <p>판정 순서:
 * <ol>
 *   <li>admin 테이블에 (활성 여부 무관) 존재하면 그 행의 활성 상태를 따른다.
 *       비활성(회수)이면 관리자가 아니다 — 부트스트랩 목록에 남아 있어도 회수를 우선한다.</li>
 *   <li>없지만 이메일이 부트스트랩 목록에 있으면, admin 테이블에 자동 등록(lazy seeding) 후 사용</li>
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
    private final Set<String> bootstrapEmails;

    public AdminAuthorizationService(AdminRepository adminRepository,
                                     @Value("${admin.bootstrap-emails:}") String bootstrapEmailsRaw) {
        this.adminRepository = adminRepository;
        this.bootstrapEmails = parseEmails(bootstrapEmailsRaw);
    }

    /**
     * clerkId/email로 관리자를 판정한다. 부트스트랩 이메일이면 최초 접근 시 자동 등록한다.
     *
     * @return 활성 관리자면 해당 Admin, 아니면 빈 Optional
     */
    @Transactional
    public Optional<Admin> resolveAdmin(String clerkId, String email) {
        // 회수 우선: clerk_id 행이 있으면(활성 여부 무관) 그 활성 상태를 따른다.
        Optional<Admin> byClerkId = adminRepository.findByClerkId(clerkId);
        if (byClerkId.isPresent()) {
            return activeOrEmpty(byClerkId.get());
        }

        String normalizedEmail = normalize(email);
        if (normalizedEmail == null || !bootstrapEmails.contains(normalizedEmail)) {
            return Optional.empty();
        }

        // 같은 이메일의 기존 행이 있으면(비활성 회수 포함) 그 활성 상태를 따른다 → 비활성이면 재등록하지 않는다.
        Optional<Admin> byEmail = adminRepository.findByEmail(normalizedEmail);
        if (byEmail.isPresent()) {
            return activeOrEmpty(byEmail.get());
        }

        return seedBootstrapAdmin(clerkId, normalizedEmail);
    }

    private Optional<Admin> seedBootstrapAdmin(String clerkId, String normalizedEmail) {
        // ON CONFLICT DO NOTHING: 동시 최초 접근/재진입에도 예외 없이 멱등하게 처리(트랜잭션 abort 차단).
        adminRepository.insertBootstrapAdminIfAbsent(clerkId, normalizedEmail, BOOTSTRAP_GRANTED_BY);

        // DO NOTHING은 행을 반환하지 않으므로 후속 SELECT.
        // 경합으로 email 충돌이 났다면 clerk_id로는 안 나오므로 email로 폴백하고, 그 결과에도 활성 판정을 적용한다.
        Optional<Admin> seeded = adminRepository.findByClerkId(clerkId)
                .or(() -> adminRepository.findByEmail(normalizedEmail));
        seeded.ifPresent(a -> log.info("[ADMIN] bootstrap 관리자 등록/확인 clerkId={} email={}", clerkId, normalizedEmail));
        return seeded.flatMap(this::activeOrEmpty);
    }

    private Optional<Admin> activeOrEmpty(Admin admin) {
        return admin.isActive() ? Optional.of(admin) : Optional.empty();
    }

    private Set<String> parseEmails(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}