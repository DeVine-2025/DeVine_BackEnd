package com.umc.devine.domain.techstack.entity;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "dev_report")
public class DevReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dev_report_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_repo_id", nullable = false)
    private GitRepoUrl gitRepoUrl;

    @Column(name = "report_content", nullable = false, length = 255)
    private String content;

}
