package com.umc.devine.domain.notice.controller;

import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.repository.NoticeRepository;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NoticeControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private NoticeRepository noticeRepository;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
    }

    private Notice save(String title, LocalDateTime start, LocalDateTime end, boolean exposed) {
        return noticeRepository.save(Notice.builder()
                .title(title)
                .content(title + " 본문")
                .displayStartAt(start)
                .displayEndAt(end)
                .isExposed(exposed)
                .build());
    }

    @Test
    @DisplayName("비로그인 상태로 공지 목록을 조회할 수 있다")
    void getsNoticeListWithoutAuthentication() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        save("게시 중", now.minusDays(1), now.plusDays(1), true);
        save("게시 예정", now.plusDays(1), now.plusDays(2), true);

        // when & then - Authorization 헤더 없이 요청
        mockMvc.perform(get("/api/v1/notices").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("NOTICE200_1"))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.content[0].title").value("게시 중"));
    }

    @Test
    @DisplayName("비로그인 상태로 공지 상세를 조회할 수 있고 본문이 포함된다")
    void getsNoticeDetailWithoutAuthentication() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        Notice notice = save("게시 중", now.minusDays(1), now.plusDays(1), true);

        // when & then
        mockMvc.perform(get("/api/v1/notices/{noticeId}", notice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTICE200_2"))
                .andExpect(jsonPath("$.result.content").value("게시 중 본문"));
    }

    @Test
    @DisplayName("비노출 공지의 상세를 조회하면 404를 반환한다")
    void returns404ForHiddenNotice() throws Exception {
        // given
        Notice notice = save("수동 비노출", null, null, false);

        // when & then
        mockMvc.perform(get("/api/v1/notices/{noticeId}", notice.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTICE404_1"));
    }
}
