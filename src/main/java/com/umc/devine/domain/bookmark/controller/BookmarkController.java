package com.umc.devine.domain.bookmark.controller;

import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.exception.code.BookmarkSuccessCode;
import com.umc.devine.domain.bookmark.service.command.BookmarkCommandService;
import com.umc.devine.domain.bookmark.service.query.BookmarkQueryService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController implements BookmarkControllerDocs {

    private final BookmarkCommandService bookmarkCommandService;
    private final BookmarkQueryService bookmarkQueryService;

    @Override
    @GetMapping
    public ApiResponse<BookmarkResDTO.BookmarkListDTO> getBookmarks(
            @CurrentMember Member member
    ) {
        BookmarkSuccessCode code = BookmarkSuccessCode.FOUND;

        return ApiResponse.onSuccess(code, bookmarkQueryService.findAllBookmarks(member));
    }

    @Override
    @PostMapping
    public ApiResponse<BookmarkResDTO.BookmarkIdDTO> createBookmark(
            @CurrentMember Member member,
            @RequestBody @Valid BookmarkReqDTO.CreateBookmarkDTO dto
    ) {
        BookmarkSuccessCode code = BookmarkSuccessCode.CREATED;

        return ApiResponse.onSuccess(code, bookmarkCommandService.createBookmark(member, dto));
    }

    @Override
    @DeleteMapping("/{bookmarkId}")
    public ApiResponse<BookmarkResDTO.BookmarkIdDTO> deleteBookmark(
            @CurrentMember Member member,
            @PathVariable("bookmarkId") Long bookmarkId
    ) {
        BookmarkSuccessCode code = BookmarkSuccessCode.DELETED;

        return ApiResponse.onSuccess(code, bookmarkCommandService.deleteBookmark(member, bookmarkId));
    }
}
