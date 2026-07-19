package com.umc.devine.admin.auth.service;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
 *   <li>admin 테이블에 활성 관리자로 존재하면 그대로 사용</li>
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
     * clerkId/email로 활성 관리자를 조회한다. 부트스트랩 이메일이면 최초 접근 시 자동 등록한다.
     *
     * @return 관리자면 해당 Admin, 아니면 빈 Optional
     */
    @Transactional
    public Optional<Admin> resolveAdmin(String clerkId, String email) {
        Optional<Admin> existing = adminRepository.findActiveByClerkId(clerkId);
        if (existing.isPresent()) {
            return existing;
        }

        if (email == null || !bootstrapEmails.contains(normalize(email))) {
            return Optional.empty();
        }

        return Optional.of(seedBootstrapAdmin(clerkId, email));
    }

    private Admin seedBootstrapAdmin(String clerkId, String email) {
        // 이미 이 이메일로 등록된 관리자가 있으면 재사용(다른 clerkId로 등록된 예외적 상황 포함)
        Optional<Admin> byEmail = adminRepository.findActiveByEmail(normalize(email));
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        try {
            Admin created = adminRepository.save(Admin.builder()
                    .clerkId(clerkId)
                    .email(normalize(email))
                    .level(AdminLevel.ADMIN)
                    .grantedBy(BOOTSTRAP_GRANTED_BY)
                    .build());
            log.info("[ADMIN] bootstrap 관리자 자동 등록 clerkId={} email={}", clerkId, normalize(email));
            return created;
        } catch (DataIntegrityViolationException e) {
            // 동시 최초 접근으로 이미 등록된 경우: 재조회하여 그 결과를 사용
            return adminRepository.findActiveByClerkId(clerkId)
                    .or(() -> adminRepository.findActiveByEmail(normalize(email)))
                    .orElseThrow(() -> e);
        }
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