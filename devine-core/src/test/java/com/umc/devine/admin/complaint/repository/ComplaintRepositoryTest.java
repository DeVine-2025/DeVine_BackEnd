package com.umc.devine.admin.complaint.repository;

import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.QComplaint;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.querydsl.core.BooleanBuilder;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplaintRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member complainant;
    private Member respondentMember;

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
    }

    private Complaint createComplaint(ComplaintTargetType targetType, Long targetId, ComplaintStatus status) {
        return complaintRepository.save(Complaint.builder()
                .complainant(complainant)
                .respondentMember(respondentMember)
                .targetType(targetType)
                .targetId(targetId)
                .reason("부적절한 콘텐츠입니다.")
                .status(status)
                .build());
    }

    @Nested
    @DisplayName("save/findById")
    class SaveAndFindTest {

        @Test
        @DisplayName("신고를 저장하고 조회할 수 있다")
        void save_success() {
            // given
            Complaint complaint = createComplaint(ComplaintTargetType.PROJECT, 1L, ComplaintStatus.PENDING);

            // when
            Complaint found = complaintRepository.findById(complaint.getId()).orElseThrow();

            // then
            assertThat(found.getComplainant().getId()).isEqualTo(complainant.getId());
            assertThat(found.getRespondentMember().getId()).isEqualTo(respondentMember.getId());
            assertThat(found.getTargetType()).isEqualTo(ComplaintTargetType.PROJECT);
            assertThat(found.getStatus()).isEqualTo(ComplaintStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("search")
    class SearchTest {

        @Test
        @DisplayName("신고 유형으로 필터링한다")
        void search_byTargetType() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.PROJECT, 2L, ComplaintStatus.PENDING);

            QComplaint complaint = QComplaint.complaint;
            BooleanBuilder predicate = new BooleanBuilder().and(complaint.targetType.eq(ComplaintTargetType.PROJECT));

            // when
            Page<Complaint> result = complaintRepository.search(predicate, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTargetType()).isEqualTo(ComplaintTargetType.PROJECT);
        }

        @Test
        @DisplayName("처리 상태로 필터링한다")
        void search_byStatus() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.CHAT, 2L, ComplaintStatus.COMPLETED);

            QComplaint complaint = QComplaint.complaint;
            BooleanBuilder predicate = new BooleanBuilder().and(complaint.status.eq(ComplaintStatus.COMPLETED));

            // when
            Page<Complaint> result = complaintRepository.search(predicate, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(ComplaintStatus.COMPLETED);
        }

        @Test
        @DisplayName("페이지네이션이 동작한다")
        void search_pagination() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.CHAT, 2L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.CHAT, 3L, ComplaintStatus.PENDING);

            // when
            Page<Complaint> result = complaintRepository.search(new BooleanBuilder(), PageRequest.of(0, 2));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("countByRespondentMemberId")
    class CountByRespondentMemberIdTest {

        @Test
        @DisplayName("피신고자의 누적 신고 건수를 센다")
        void countByRespondentMemberId_success() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.PROJECT, 2L, ComplaintStatus.COMPLETED);

            // when
            long count = complaintRepository.countByRespondentMemberId(respondentMember.getId());

            // then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findByRespondentMemberIdOrderByCreatedAtDesc")
    class FindByRespondentMemberIdOrderByCreatedAtDescTest {

        @Test
        @DisplayName("피신고자의 신고/제재 이력을 최신순으로 조회한다")
        void findByRespondentMemberIdOrderByCreatedAtDesc_success() {
            // given
            createComplaint(ComplaintTargetType.CHAT, 1L, ComplaintStatus.PENDING);
            createComplaint(ComplaintTargetType.PROJECT, 2L, ComplaintStatus.COMPLETED);

            // when
            List<Complaint> result = complaintRepository.findByRespondentMemberIdOrderByCreatedAtDesc(respondentMember.getId());

            // then
            assertThat(result).hasSize(2);
        }
    }
}
