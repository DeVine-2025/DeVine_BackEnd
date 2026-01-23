package com.umc.devine.domain.bookmark.controller;

import com.umc.devine.domain.bookmark.dto.BookmarkReqDTO;
import com.umc.devine.domain.bookmark.dto.BookmarkResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Bookmark", description = "북마크 관련 API")
public interface BookmarkControllerDocs {

    @Operation(summary = "북마크 전체 조회 API", description = "내 북마크 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<BookmarkResDTO.BookmarkListDTO> getBookmarks();

    @Operation(summary = "북마크 저장 API", description = "프로젝트 또는 개발자를 북마크에 저장하는 API입니다. targetType은 PROJECT 또는 DEVELOPER입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 북마크에 추가되어 있습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<BookmarkResDTO.BookmarkIdDTO> createBookmark(
            @Valid @RequestBody BookmarkReqDTO.CreateBookmarkDTO dto
    );

    @Operation(summary = "북마크 취소 API", description = "북마크를 삭제하는 API입니다. bookmarkId를 path variable로 전달해주세요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "북마크에 대한 접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 북마크를 찾을 수 없습니다.")
    })
    ApiResponse<BookmarkResDTO.BookmarkIdDTO> deleteBookmark(Long bookmarkId);
}
