package com.umc.devine.global.util;

public class GitUrlParser {

    private GitUrlParser() {
    }

    /**
     * Git URL에서 레포지토리 이름만 추출
     * 예: https://github.com/user/repo-name -> repo-name
     */
    public static String extractRepoName(String gitUrl) {
        if (gitUrl == null || gitUrl.isBlank()) {
            return "";
        }

        String url = removeGitExtension(gitUrl);
        int lastSlashIndex = url.lastIndexOf('/');
        return lastSlashIndex >= 0 ? url.substring(lastSlashIndex + 1) : url;
    }

    /**
     * Git URL에서 사용자명/레포지토리 형태로 추출
     * 예: https://github.com/user/repo-name -> user/repo-name
     */
    public static String extractOwnerAndRepo(String gitUrl) {
        if (gitUrl == null || gitUrl.isBlank()) {
            return "";
        }

        String url = removeGitExtension(gitUrl);

        // github.com/ 또는 gitlab.com/ 등 이후의 경로 추출
        String[] hostPatterns = {"github.com/", "gitlab.com/", "bitbucket.org/"};
        for (String pattern : hostPatterns) {
            int hostIndex = url.indexOf(pattern);
            if (hostIndex >= 0) {
                return url.substring(hostIndex + pattern.length());
            }
        }

        // 패턴이 없으면 마지막 두 경로 세그먼트 반환 시도
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }

        return extractRepoName(gitUrl);
    }

    private static String removeGitExtension(String url) {
        return url.endsWith(".git") ? url.substring(0, url.length() - 4) : url;
    }
}
