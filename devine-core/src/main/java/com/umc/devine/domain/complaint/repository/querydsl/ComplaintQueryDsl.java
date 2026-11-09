package com.umc.devine.domain.complaint.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.complaint.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ComplaintQueryDsl {

    Page<Complaint> search(Predicate predicate, Pageable pageable);
}
