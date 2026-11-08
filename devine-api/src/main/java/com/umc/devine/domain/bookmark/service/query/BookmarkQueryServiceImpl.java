package com.umc.devine.domain.bookmark.service.query;

import com.umc.devine.domain.bookmark.converter.BookmarkConverter;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkQueryServiceImpl implements BookmarkQueryService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;

    @Override
    public BookmarkResDTO.BookmarkListDTO findAllBookmarks(Member member) {
        List<Bookmark> bookmarks = hideInvisibleProjects(bookmarkRepository.findAllByMember(member));

        // DEVELOPER 타입 북마크의 targetId를 일괄 조회하여 nickname 매핑
        List<Long> developerTargetIds = bookmarks.stream()
                .filter(b -> b.getTargetType() == BookmarkType.DEVELOPER)
                .map(Bookmark::getTargetId)
                .distinct()
                .toList();

        Map<Long, String> nicknameMap = memberRepository.findAllById(developerTargetIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));

        return BookmarkConverter.toBookmarkListDTO(bookmarks, nicknameMap);
    }

    private List<Bookmark> hideInvisibleProjects(List<Bookmark> bookmarks) {
        List<Long> projectIds = bookmarks.stream()
                .filter(bookmark -> bookmark.getTargetType() == BookmarkType.PROJECT)
                .map(Bookmark::getTargetId)
                .distinct()
                .toList();
        if (projectIds.isEmpty()) {
            return bookmarks;
        }

        Set<Long> visibleProjectIds = projectRepository.findVisibleByIdIn(projectIds).stream()
                .map(Project::getId)
                .collect(Collectors.toSet());

        return bookmarks.stream()
                .filter(bookmark -> bookmark.getTargetType() != BookmarkType.PROJECT
                        || visibleProjectIds.contains(bookmark.getTargetId()))
                .toList();
    }
}
