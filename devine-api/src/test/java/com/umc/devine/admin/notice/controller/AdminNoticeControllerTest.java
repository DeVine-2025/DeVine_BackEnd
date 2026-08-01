package com.umc.devine.admin.notice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.notice.dto.AdminNoticeReqDTO;
import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.repository.NoticeRepository;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminNoticeControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();

        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("clerk_admin")
                .email("admin@example.com")
                .name("관리자")
                .level(AdminLevel.ADMIN)
                .build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Notice saveNotice() {
        return noticeRepository.save(Notice.builder()
                .title("원본 제목")
                .content("원본 내용")
                .isExposed(true)
                .build());
    }

    @Nested
    @DisplayName("POST /admin/v1/notices")
    class CreateNotice {

        @Test
        @DisplayName("공지를 등록하면 ADMINNOTICE201_1을 반환한다")
        void createsNotice() throws Exception {
            // given
            var request = new AdminNoticeReqDTO.CreateNoticeReq(
                    "점검 안내", "7월 30일 점검", LocalDateTime.now(), LocalDateTime.now().plusDays(3), true);

            // when & then
            mockMvc.perform(post("/admin/v1/notices")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("ADMINNOTICE201_1"))
                    .andExpect(jsonPath("$.result.title").value("점검 안내"))
                    .andExpect(jsonPath("$.result.displayStatus").value("DISPLAYING"));
        }

        @Test
        @DisplayName("제목이 없으면 400을 반환한다")
        void returns400WhenTitleMissing() throws Exception {
            // given
            String body = objectMapper.writeValueAsString(Map.of("content", "제목 없는 공지"));

            // when & then
            mockMvc.perform(post("/admin/v1/notices")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("내용이 없으면 400을 반환한다")
        void returns400WhenContentMissing() throws Exception {
            // given
            String body = objectMapper.writeValueAsString(Map.of("title", "내용 없는 공지"));

            // when & then
            mockMvc.perform(post("/admin/v1/notices")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }

        @Test
        @DisplayName("게시 기간이 역전되면 NOTICE400_1을 반환한다")
        void returns400WhenPeriodReversed() throws Exception {
            // given
            var request = new AdminNoticeReqDTO.CreateNoticeReq(
                    "잘못된 기간", "내용", LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(1), true);

            // when & then
            mockMvc.perform(post("/admin/v1/notices")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("NOTICE400_1"));
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/notices")
    class GetNotices {

        @Test
        @DisplayName("비노출 공지까지 포함해 조회한다")
        void includesHiddenNotices() throws Exception {
            // given
            noticeRepository.save(Notice.builder()
                    .title("비노출 공지").content("내용").isExposed(false).build());

            // when & then
            mockMvc.perform(get("/admin/v1/notices")
                            .with(authentication(adminAuth))
                            .param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ADMINNOTICE200_1"))
                    .andExpect(jsonPath("$.result.totalElements").value(1))
                    .andExpect(jsonPath("$.result.content[0].displayStatus").value("HIDDEN"));
        }
    }

    @Nested
    @DisplayName("GET /admin/v1/notices/{noticeId}")
    class GetNotice {

        @Test
        @DisplayName("존재하지 않는 공지를 조회하면 404를 반환한다")
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(get("/admin/v1/notices/{noticeId}", 999_999L)
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTICE404_1"));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/v1/notices/{noticeId}")
    class UpdateNotice {

        @Test
        @DisplayName("일부 필드만 보내면 나머지는 유지된다")
        void updatesPartially() throws Exception {
            // given
            Notice notice = saveNotice();
            String body = objectMapper.writeValueAsString(Map.of("isExposed", false));

            // when & then
            mockMvc.perform(patch("/admin/v1/notices/{noticeId}", notice.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ADMINNOTICE200_3"))
                    .andExpect(jsonPath("$.result.title").value("원본 제목"))
                    .andExpect(jsonPath("$.result.isExposed").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /admin/v1/notices/{noticeId}")
    class DeleteNotice {

        @Test
        @DisplayName("공지를 삭제하면 ADMINNOTICE200_4를 반환한다")
        void deletesNotice() throws Exception {
            // given
            Notice notice = saveNotice();

            // when & then
            mockMvc.perform(delete("/admin/v1/notices/{noticeId}", notice.getId())
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ADMINNOTICE200_4"));
        }

        @Test
        @DisplayName("존재하지 않는 공지를 삭제하면 404를 반환한다")
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(delete("/admin/v1/notices/{noticeId}", 999_999L)
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTICE404_1"));
        }
    }
}
