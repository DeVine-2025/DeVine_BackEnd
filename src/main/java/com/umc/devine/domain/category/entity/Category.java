package com.umc.devine.domain.category.entity;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "domain")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "domain_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_genre", nullable = false)
    private CategoryGenre genre;
}
