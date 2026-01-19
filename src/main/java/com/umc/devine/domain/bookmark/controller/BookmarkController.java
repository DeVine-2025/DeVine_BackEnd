package com.umc.devine.domain.bookmark.controller;

import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.domain.bookmark.exception.code.BookmarkSuccessCode;
import com.umc.devine.domain.bookmark.service.command.BookmarkCommandService;
import com.umc.devine.domain.bookmark.service.query.BookmarkQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
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
    public ApiResponse<BookmarkResDTO.BookmarkListDTO> getBookmarks() {
        BookmarkSuccessCode code = BookmarkSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, bookmarkQueryService.findAllBookmarks(memberId));
    }

    @Override
    @PostMapping
    public ApiResponse<BookmarkResDTO.BookmarkIdDTO> createBookmark(
            @RequestBody @Valid BookmarkReqDTO.CreateBookmarkDTO dto
    ) {
        BookmarkSuccessCode code = BookmarkSuccessCode.CREATED;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, bookmarkCommandService.createBookmark(memberId, dto));
    }

    @Override
    @DeleteMapping("/{bookmarkId}")
    public ApiResponse<BookmarkResDTO.BookmarkIdDTO> deleteBookmark(
            @PathVariable("bookmarkId") Long bookmarkId
    ) {
        BookmarkSuccessCode code = BookmarkSuccessCode.DELETED;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, bookmarkCommandService.deleteBookmark(memberId, bookmarkId));
    }
}
