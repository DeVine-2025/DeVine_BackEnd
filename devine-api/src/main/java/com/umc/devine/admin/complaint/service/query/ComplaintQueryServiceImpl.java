package com.umc.devine.admin.complaint.service.query;

import com.querydsl.core.BooleanBuilder;
import com.umc.devine.admin.complaint.converter.ComplaintConverter;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.entity.Complaint;
import com.umc.devine.admin.complaint.entity.QComplaint;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.admin.complaint.exception.ComplaintException;
import com.umc.devine.admin.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.admin.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintQueryServiceImpl implements ComplaintQueryService {

    private static final String DELETED_CONTENT_PLACEHOLDER = "삭제된 콘텐츠입니다";

    private final ComplaintRepository complaintRepository;
    private final ProjectRepository projectRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public PagedResponse<ComplaintResDTO.ComplaintSummaryDTO> getComplaintList(ComplaintReqDTO.SearchReq request) {
        BooleanBuilder predicate = buildPredicate(request);
        Page<Complaint> page = complaintRepository.search(predicate, request.toPageable());
        LocalDateTime now = LocalDateTime.now();

        List<ComplaintResDTO.ComplaintSummaryDTO> content = page.getContent().stream()
                .map(complaint -> ComplaintConverter.toComplaintSummaryDTO(complaint, ComplaintConverter.isSlaExceeded(complaint, now)))
                .toList();

        return PagedResponse.of(page, content);
    }

    private BooleanBuilder buildPredicate(ComplaintReqDTO.SearchReq request) {
        QComplaint complaint = QComplaint.complaint;
        BooleanBuilder builder = new BooleanBuilder();

        if (request.targetType() != null) {
            builder.and(complaint.targetType.eq(request.targetType()));
        }
        if (request.status() != null) {
            builder.and(complaint.status.eq(request.status()));
        }
        if (request.fromDate() != null) {
            builder.and(complaint.createdAt.goe(request.fromDate().atStartOfDay()));
        }
        if (request.toDate() != null) {
            builder.and(complaint.createdAt.loe(request.toDate().atTime(LocalTime.MAX)));
        }
        return builder;
    }

    @Override
    public ComplaintResDTO.ComplaintDetailRes getComplaintDetail(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintException(ComplaintErrorReason.COMPLAINT_NOT_FOUND));

        String content = resolveContent(complaint);
        ComplaintResDTO.RespondentHistoryRes respondentHistory = getRespondentHistory(complaint.getRespondentMember().getId());

        return ComplaintConverter.toComplaintDetailRes(
                complaint, content, respondentHistory.respondentComplaintCount(), respondentHistory.respondentHistory());
    }

    @Override
    public ComplaintResDTO.RespondentHistoryRes getRespondentHistory(Long memberId) {
        long respondentComplaintCount = complaintRepository.countByRespondentMemberId(memberId);
        LocalDateTime now = LocalDateTime.now();
        List<ComplaintResDTO.ComplaintSummaryDTO> history = complaintRepository
                .findByRespondentMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(r -> ComplaintConverter.toComplaintSummaryDTO(r, ComplaintConverter.isSlaExceeded(r, now)))
                .toList();
        return ComplaintResDTO.RespondentHistoryRes.builder()
                .respondentComplaintCount(respondentComplaintCount)
                .respondentHistory(history)
                .build();
    }

    private String resolveContent(Complaint complaint) {
        if (complaint.getTargetType() == ComplaintTargetType.PROJECT) {
            return projectRepository.findById(complaint.getTargetId())
                    .map(project -> project.getStatus() == ProjectStatus.DELETED
                            ? DELETED_CONTENT_PLACEHOLDER
                            : project.getContent())
                    .orElse(DELETED_CONTENT_PLACEHOLDER);
        }
        if (complaint.getTargetType() == ComplaintTargetType.CHAT) {
            // 신고 대상은 채팅방 전체가 아니라 신고당한 그 메시지 한 건(target_id = chat_message_id)이라 DB에서 바로 조회한다.
            return chatMessageRepository.findById(complaint.getTargetId())
                    .map(ChatMessage::getContent)
                    .orElse(DELETED_CONTENT_PLACEHOLDER);
        }
        return null;
    }
}
