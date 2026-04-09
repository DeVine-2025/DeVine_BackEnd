package com.umc.devine.domain.payment.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.enums.PgProvider;

public interface PaymentQueryService {
    PaymentResDTO.PaymentListDTO getMyPayments(Member member);
    PaymentResDTO.ChannelKeyDTO getChannelKey(PgProvider pgProvider);
}
