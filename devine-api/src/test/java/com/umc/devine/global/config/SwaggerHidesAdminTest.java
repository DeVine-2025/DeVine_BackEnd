package com.umc.devine.global.config;

import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwaggerHidesAdminTest extends ControllerIntegrationTestSupport {

    @Test
    @DisplayName("OpenAPI 문서에 /admin 경로가 노출되지 않는다")
    void adminPathsAreHidden() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // 대조군: 공개 경로는 실제로 문서에 있어야 한다(빈 문서로 인한 허위 통과 방지)
                .andExpect(jsonPath("$.paths['/api/v1/notifications']").exists())
                .andExpect(jsonPath("$.paths['/admin/v1/payments']").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/v1/payments/{paymentId}']").doesNotExist())
                .andExpect(jsonPath("$.paths['/admin/v1/payments/{paymentId}/refund']").doesNotExist());
    }
}
