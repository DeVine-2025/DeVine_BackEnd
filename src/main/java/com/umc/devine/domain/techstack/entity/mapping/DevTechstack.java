package com.umc.devine.domain.techstack.entity.mapping;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.entity.Techstack;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "dev_techstack")
public class DevTechstack {

    @EmbeddedId
    private DevTechstackId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @MapsId("techstackId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "techstack_id")
    private Techstack techstack;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class DevTechstackId implements Serializable {
        private Long memberId;
        private Long techstackId;
    }
}
