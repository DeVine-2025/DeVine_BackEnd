package com.umc.devine.infrastructure.sse.controller;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseControllerDocs {

    SseEmitter subscribe(String lastEventId);
}
