package com.umc.devine.domain.member.service.query;

import com.umc.devine.domain.member.converter.MemberConverter;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.converter.ProjectConverter;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import com.umc.devine.domain.project.repository.ProjectImageRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.ReportTechStackResDTO;
import com.umc.devine.domain.techstack.entity.DevReport;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.entity.mapping.ReportTechstack;
import com.umc.devine.domain.techstack.repository.DevReportRepository;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.ReportTechstackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final DevTechstackRepository devTechstackRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final DevReportRepository devReportRepository;
    private final ReportTechstackRepository reportTechstackRepository;

    @Override
    public MemberResDTO.MemberDetailDTO findMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
        return MemberConverter.toMemberDetailDTO(member);
    }

    @Override
    public MemberResDTO.UserProfileDTO findMemberByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));
        List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMember(member);

        return MemberConverter.toUserProfileDTO(member, devTechstacks);
    }

    @Override
    public ProjectResDTO.ProjectListDTO findMyProjects(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        List<Project> projects = projectRepository.findByMember(member);

        List<ProjectResDTO.ProjectDetailDTO> projectList = projects.stream().map(project -> {
            List<ProjectImage> images = projectImageRepository.findByProject(project);
            return ProjectConverter.toProjectDetail(project, images);
        }).collect(Collectors.toList());

        return ProjectConverter.toProjectList(projectList);
    }

    @Override
    public MemberResDTO.NicknameDuplicateDTO checkNicknameDuplicate(String nickname) {
        boolean isDuplicate = memberRepository.existsByNickname(nickname);
        return MemberResDTO.NicknameDuplicateDTO.builder()
                .nickname(nickname)
                .isDuplicate(isDuplicate)
                .build();
    }

    @Override
    public DevReportResDTO.ReportListDTO findMyReports(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        List<GitRepoUrl> gitRepoUrls = gitRepoUrlRepository.findAllByMember(member);
        List<DevReport> devReports = devReportRepository.findAllByGitRepoUrlIn(gitRepoUrls);

        List<DevReportResDTO.ReportDTO> reportDTOs = devReports.stream().map(report -> {
            List<ReportTechstack> reportTechstacks = reportTechstackRepository.findAllByDevReport(report);

            List<ReportTechStackResDTO.ReportTechstackDTO> techstackDTOs = reportTechstacks.stream()
                    .map(rt -> ReportTechStackResDTO.ReportTechstackDTO.builder()
                            .techstackName(rt.getTechstack().getName().toString())
                            .techGenre(rt.getTechstack().getGenre().toString())
                            .rate(rt.getRate())
                            .build())
                    .collect(Collectors.toList());

            return DevReportResDTO.ReportDTO.builder()
                    .reportId(report.getId())
                    .gitUrl(report.getGitRepoUrl().getGitUrl())
                    .content(report.getContent())
                    .techstacks(techstackDTOs)
                    .createdAt(report.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        return DevReportResDTO.ReportListDTO.builder()
                .reports(reportDTOs)
                .build();
    }

    @Override
    public DevReportResDTO.ReportListDTO findReportsByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        List<GitRepoUrl> gitRepoUrls = gitRepoUrlRepository.findAllByMember(member);
        List<DevReport> devReports = devReportRepository.findAllByGitRepoUrlIn(gitRepoUrls);

        List<DevReportResDTO.ReportDTO> reportDTOs = devReports.stream().map(report -> {
            List<ReportTechstack> reportTechstacks = reportTechstackRepository.findAllByDevReport(report);

            List<ReportTechStackResDTO.ReportTechstackDTO> techstackDTOs = reportTechstacks.stream()
                    .map(rt -> ReportTechStackResDTO.ReportTechstackDTO.builder()
                            .techstackName(rt.getTechstack().getName().toString())
                            .techGenre(rt.getTechstack().getGenre().toString())
                            .rate(rt.getRate())
                            .build())
                    .collect(Collectors.toList());

            return DevReportResDTO.ReportDTO.builder()
                    .reportId(report.getId())
                    .gitUrl(report.getGitRepoUrl().getGitUrl())
                    .content(report.getContent())
                    .techstacks(techstackDTOs)
                    .createdAt(report.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        return DevReportResDTO.ReportListDTO.builder()
                .reports(reportDTOs)
                .build();
    }
}
