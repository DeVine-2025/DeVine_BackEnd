package com.umc.devine.admin.coupon.service.query;

import com.umc.devine.admin.coupon.converter.AdminCouponConverter;
import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.dto.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCouponQueryServiceImpl implements AdminCouponQueryService {

    private final CouponRepository couponRepository;

    @Override
    public PagedResponse<AdminCouponResDTO.CouponDTO> getCoupons(PageRequest pageRequest) {
        Page<Coupon> page = couponRepository.findAllByOrderByCreatedAtDesc(pageRequest.toPageable());
        List<AdminCouponResDTO.CouponDTO> content = page.getContent().stream()
                .map(AdminCouponConverter::toCouponDTO)
                .toList();
        return PagedResponse.of(page, content);
    }

    @Override
    public List<AdminCouponResDTO.UsageStatDTO> getUsageStats(Long couponId) {
        if (couponId != null) {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new CouponException(CouponErrorReason.COUPON_NOT_FOUND));
            return List.of(AdminCouponConverter.toUsageStatDTO(coupon));
        }
        return couponRepository.findAll().stream()
                .map(AdminCouponConverter::toUsageStatDTO)
                .toList();
    }
}
