package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.ComplaintHistory;
import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplaintHistoryRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Complaint complaint;
    private Member resolver;

    @BeforeEach
    void setUp() {
        Member complainant = memberRepository.save(Member.builder()
                .clerkId("clerk_complainant")
                .name("신고자")
                .nickname("complainant")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        Member respondentMember = memberRepository.save(Member.builder()
                .clerkId("clerk_respondent")
                .name("피신고자")
                .nickname("respondent")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        resolver = memberRepository.save(Member.builder()
                .clerkId("clerk_admin")
                .name("관리자")
                .nickname("admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        complaint = complaintRepository.save(Complaint.builder()
                .complainant(complainant)
                .respondentMember(respondentMember)
                .targetType(ComplaintTargetType.CHAT)
                .targetId(1L)
                .reason("부적절한 콘텐츠입니다.")
                .status(ComplaintStatus.PENDING)
                .build());
    }

    @Nested
    @DisplayName("save/findById")
    class SaveAndFindTest {

        @Test
        @DisplayName("이력을 저장하고 조회할 수 있다")
        void save_success() {
            // given
            ComplaintHistory history = complaintHistoryRepository.save(ComplaintHistory.builder()
                    .complaint(complaint)
                    .status(ComplaintStatus.IN_REVIEW)
                    .resolver(resolver)
                    .build());

            // when
            ComplaintHistory found = complaintHistoryRepository.findById(history.getId()).orElseThrow();

            // then
            assertThat(found.getComplaint().getId()).isEqualTo(complaint.getId());
            assertThat(found.getStatus()).isEqualTo(ComplaintStatus.IN_REVIEW);
            assertThat(found.getResolver().getId()).isEqualTo(resolver.getId());
        }
    }

    @Nested
    @DisplayName("findByComplaintIdOrderByCreatedAtDesc")
    class FindByComplaintIdOrderByCreatedAtDescTest {

        @Test
        @DisplayName("신고ID로 이력을 최신순으로 조회한다")
        void findByComplaintIdOrderByCreatedAtDesc_success() {
            // given
            complaintHistoryRepository.save(ComplaintHistory.builder()
                    .complaint(complaint)
                    .status(ComplaintStatus.IN_REVIEW)
                    .resolver(resolver)
                    .build());
            complaintHistoryRepository.save(ComplaintHistory.builder()
                    .complaint(complaint)
                    .status(ComplaintStatus.COMPLETED)
                    .action(ComplaintAction.WARNING)
                    .resolutionReason("확인 결과 규정 위반")
                    .resolver(resolver)
                    .build());

            // when
            List<ComplaintHistory> result = complaintHistoryRepository.findByComplaintIdOrderByCreatedAtDesc(complaint.getId());

            // then
            assertThat(result).hasSize(2);
        }
    }
}
