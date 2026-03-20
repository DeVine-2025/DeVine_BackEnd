package com.umc.devine.domain.member.repository;

import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TermsRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private TermsRepository termsRepository;

    @Nested
    @DisplayName("findAllByRequired")
    class FindAllByRequiredTest {

        @Test
        @DisplayName("필수 약관만 조회한다")
        void findAllByRequired_required() {
            // given - V3 시드 데이터로 필수 약관 2건이 이미 존재
            // 선택 약관 추가
            termsRepository.save(Terms.builder()
                    .title("마케팅 수신 동의")
                    .content("선택 약관 내용")
                    .required(false)
                    .build());

            // when
            List<Terms> result = termsRepository.findAllByRequired(true);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(Terms::getRequired);
        }

        @Test
        @DisplayName("선택 약관만 조회한다")
        void findAllByRequired_optional() {
            // given
            termsRepository.save(Terms.builder()
                    .title("마케팅 수신 동의")
                    .content("선택 약관 내용")
                    .required(false)
                    .build());

            // when
            List<Terms> result = termsRepository.findAllByRequired(false);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).allMatch(terms -> !terms.getRequired());
        }

        @Test
        @DisplayName("해당 조건의 약관이 없으면 빈 리스트를 반환한다")
        void findAllByRequired_empty() {
            // when - V3 시드 데이터에 선택 약관은 없음
            List<Terms> result = termsRepository.findAllByRequired(false);

            // then
            assertThat(result).isEmpty();
        }
    }
}
