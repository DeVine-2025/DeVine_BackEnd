package com.umc.devine.infrastructure.outbox.payload;

import java.util.List;

public record WithdrawalRefundMailPayload(List<Long> refundIds) {
}
