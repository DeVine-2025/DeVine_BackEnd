package com.umc.devine.domain.project.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectEmbeddingEvent {

    private final Long projectId;
    private final String content;
}
