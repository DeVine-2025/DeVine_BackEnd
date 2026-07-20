package com.umc.devine.admin.complaint.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.umc.devine.global.security.ClerkPrincipal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ComplaintControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member complainant;
    private Member respondentMember;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        complainant = memberRepository.save(Member.builder()
                .clerkId("clerk_complainant")
                .name("신고자")
                .nickname("complainant")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        respondentMember = memberRepository.save(Member.builder()
                .clerkId("clerk_respondent")
                .name("피신고자")
                .nickname("respondent")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        Member admin = memberRepository.save(Member.builder()
                .clerkId("clerk_admin")
                .name("관리자")
                .nickname("admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        ClerkPrincipal principal = new ClerkPrincipal("clerk_admin", "admin@example.com", "관리자", null);
        adminAuth = new UsernamePasswordAuthenticationToken(principal, null, java.util.Collections.emptyList());
    }

    private Complaint createComplaint(ComplaintStatus status) {
        return complaintRepository.save(Complaint.builder()
                .complainant(complainant)
                .respondentMember(respondentMember)
                .targetType(ComplaintTargetType.CHAT)
                .targetId(1L)
                .reason("부적절한 콘텐츠입니다.")
                .status(status)
                .build());
    }

    @Nested
    @DisplayName("신고 목록 조회")
    class GetComplaintListTest {

        @Test
        @DisplayName("인증 없이도 신고 목록을 조회할 수 있다")
        void getComplaintList_success() throws Exception {
            // given
            createComplaint(ComplaintStatus.PENDING);

            // when & then
            mockMvc.perform(get("/api/v1/admin/complaints")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.content").isArray());
        }
    }

    @Nested
    @DisplayName("신고 상세 조회")
    class GetComplaintDetailTest {

        @Test
        @DisplayName("신고 상세를 조회한다")
        void getComplaintDetail_success() throws Exception {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.PENDING);

            // when & then
            mockMvc.perform(get("/api/v1/admin/complaints/{complaintId}", complaint.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.complaintId").value(complaint.getId()));
        }

        @Test
        @DisplayName("존재하지 않는 신고ID면 404를 반환한다")
        void getComplaintDetail_notFound() throws Exception {
            mockMvc.perform(get("/api/v1/admin/complaints/{complaintId}", 999999L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("신고 처리 상태 변경")
    class UpdateStatusTest {

        @Test
        @DisplayName("상태 변경에 성공한다")
        void updateStatus_success() throws Exception {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.PENDING);
            ComplaintReqDTO.UpdateStatusReq dto = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.IN_REVIEW)
                    .build();

            // when & then
            mockMvc.perform(patch("/api/v1/admin/complaints/{complaintId}/status", complaint.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.status").value("IN_REVIEW"));
        }

        @Test
        @DisplayName("처리완료 시 처리 사유가 없으면 400을 반환한다")
        void updateStatus_missingReason() throws Exception {
            // given
            Complaint complaint = createComplaint(ComplaintStatus.PENDING);
            ComplaintReqDTO.UpdateStatusReq dto = ComplaintReqDTO.UpdateStatusReq.builder()
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.WARNING)
                    .build();

            // when & then
            mockMvc.perform(patch("/api/v1/admin/complaints/{complaintId}/status", complaint.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }
}
