package com.umc.devine.support;

import com.umc.devine.testconfig.CoreTestApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CoreTestApplication.class)
public abstract class CoreIntegrationTestSupport extends IntegrationTestSupport {
}
