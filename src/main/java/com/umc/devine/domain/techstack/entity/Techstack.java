package com.umc.devine.domain.techstack.entity;

import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "techstack")
public class Techstack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teckstack_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "teck_genre", nullable = false)
    private TechGenre genre;

    @Enumerated(EnumType.STRING)
    @Column(name = "teckstack_name", nullable = false)
    private TechName name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_stack")
    private Techstack parentStack;

}
