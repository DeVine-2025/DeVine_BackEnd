package com.umc.devine.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class ControllerIntegrationTestSupport extends IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

}
