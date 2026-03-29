package com.umc.devine.domain.payment.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.converter.PaymentConverter;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.enums.PgProvider;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final String nhnKcpChannelKey;
    private final String kgInicisChannelKey;
    private final String kakaopayChannelKey;

    public PaymentQueryServiceImpl(
            PaymentRepository paymentRepository,
            @Value("${portone.channel-key.nhn-kcp}") String nhnKcpChannelKey,
            @Value("${portone.channel-key.kg-inicis}") String kgInicisChannelKey,
            @Value("${portone.channel-key.kakaopay}") String kakaopayChannelKey
    ) {
        this.paymentRepository = paymentRepository;
        this.nhnKcpChannelKey = nhnKcpChannelKey;
        this.kgInicisChannelKey = kgInicisChannelKey;
        this.kakaopayChannelKey = kakaopayChannelKey;
    }

    @Override
    public PaymentResDTO.PaymentListDTO getMyPayments(Member member) {
        List<Payment> payments = paymentRepository.findAllByMemberWithTransactions(member);
        return PaymentConverter.toPaymentListDTO(payments);
    }

    @Override
    public PaymentResDTO.ChannelKeyDTO getChannelKey(PgProvider pgProvider) {
        String channelKey = switch (pgProvider) {
            case NHN_KCP -> nhnKcpChannelKey;
            case KG_INICIS -> kgInicisChannelKey;
            case KAKAOPAY -> kakaopayChannelKey;
        };
        if (channelKey == null || channelKey.isBlank()) {
            throw new PaymentException(PaymentErrorReason.UNSUPPORTED_PAYMENT_METHOD);
        }
        return PaymentResDTO.ChannelKeyDTO.builder()
                .channelKey(channelKey)
                .pgProvider(pgProvider)
                .build();
    }
}
