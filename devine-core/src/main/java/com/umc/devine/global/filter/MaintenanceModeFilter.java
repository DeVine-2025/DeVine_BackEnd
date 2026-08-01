package com.umc.devine.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.maintenance.dto.MaintenanceState;
import com.umc.devine.domain.maintenance.exception.code.MaintenanceErrorReason;
import com.umc.devine.domain.maintenance.service.MaintenanceModeService;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 점검 모드가 켜져 있으면 일반 요청을 503 점검 안내로 응답한다.
 *
 * <p>이 필터는 Security 체인보다 <b>앞에서</b> 동작한다. 그래야 토큰이 없거나 만료된 요청도
 * 401이 아니라 점검 안내를 받는다. 대신 SecurityContext가 아직 비어 있으므로
 * <b>인증 정보를 볼 수 없고, 볼 필요도 없다</b> — 관리자 예외는 role이 아니라
 * 통과 경로 목록({@code /admin/**})으로만 표현된다. 덕분에 관리자 인가 방식이 바뀌어도
 * 이 필터는 영향을 받지 않는다.
 *
 * <p>통과 경로는 앱마다 다르므로 생성자로 주입받는다.
 */
public class MaintenanceModeFilter extends OncePerRequestFilter {

    /** 종료 예정 시각이 없거나 이미 지난 경우 클라이언트에게 안내할 기본 재시도 간격. */
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 300L;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final MaintenanceModeService maintenanceModeService;
    private final ObjectMapper objectMapper;
    private final List<String> allowedPathPatterns;

    public MaintenanceModeFilter(MaintenanceModeService maintenanceModeService,
                                 ObjectMapper objectMapper,
                                 List<String> allowedPathPatterns) {
        this.maintenanceModeService = maintenanceModeService;
        this.objectMapper = objectMapper;
        this.allowedPathPatterns = List.copyOf(allowedPathPatterns);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MaintenanceState state = maintenanceModeService.getState();

        if (!state.enabled() || isAllowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeMaintenanceNotice(response, state);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        return allowedPathPatterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private void writeMaintenanceNotice(HttpServletResponse response, MaintenanceState state) throws IOException {
        response.setStatus(MaintenanceErrorReason.UNDER_MAINTENANCE.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds(state.estimatedEndAt())));

        ApiResponse<MaintenanceState> body =
                ApiResponse.onFailure(MaintenanceErrorReason.UNDER_MAINTENANCE, state);
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * 종료 예정 시각까지 남은 초. 시각이 없거나 이미 지났으면 기본값을 쓴다.
     * (지난 시각을 그대로 쓰면 음수가 되어 클라이언트가 즉시 재시도를 반복한다)
     */
    private long retryAfterSeconds(LocalDateTime estimatedEndAt) {
        if (estimatedEndAt == null) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
        long seconds = Duration.between(LocalDateTime.now(), estimatedEndAt).toSeconds();
        return seconds > 0 ? seconds : DEFAULT_RETRY_AFTER_SECONDS;
    }
}
