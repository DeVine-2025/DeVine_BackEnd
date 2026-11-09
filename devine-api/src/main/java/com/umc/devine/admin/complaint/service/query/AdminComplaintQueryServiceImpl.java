package com.umc.devine.admin.complaint.service.query;

import com.querydsl.core.BooleanBuilder;
import com.umc.devine.admin.complaint.converter.ComplaintConverter;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.complaint.entity.Complaint;
import com.umc.devine.domain.complaint.entity.QComplaint;
import com.umc.devine.domain.complaint.exception.ComplaintException;
import com.umc.devine.domain.complaint.exception.code.ComplaintErrorReason;
import com.umc.devine.domain.complaint.repository.ComplaintRepository;
import com.umc.devine.domain.complaint.helper.ComplaintTargetReader;
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
public class AdminComplaintQueryServiceImpl implements AdminComplaintQueryService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintTargetReader targetReader;

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

        String content = targetReader.resolveContent(complaint);

        if (complaint.getRespondentMember() == null) {
            return ComplaintConverter.toComplaintDetailRes(complaint, content, 0, List.of());
        }

        Long respondentMemberId = complaint.getRespondentMember().getId();
        long respondentComplaintCount = complaintRepository.countByRespondentMemberId(respondentMemberId);
        LocalDateTime now = LocalDateTime.now();
        List<ComplaintResDTO.ComplaintSummaryDTO> history = complaintRepository
                .findByRespondentMemberIdOrderByCreatedAtDesc(respondentMemberId).stream()
                .map(r -> ComplaintConverter.toComplaintSummaryDTO(r, ComplaintConverter.isSlaExceeded(r, now)))
                .toList();

        return ComplaintConverter.toComplaintDetailRes(complaint, content, respondentComplaintCount, history);
    }

}
