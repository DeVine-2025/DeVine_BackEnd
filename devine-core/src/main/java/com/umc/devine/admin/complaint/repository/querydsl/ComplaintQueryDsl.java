package com.umc.devine.admin.complaint.repository.querydsl;

import com.querydsl.core.types.Predicate;
import com.umc.devine.admin.complaint.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ComplaintQueryDsl {

    Page<Complaint> search(Predicate predicate, Pageable pageable);
}
