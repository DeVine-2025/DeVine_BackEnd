package com.umc.devine.global.external.clerk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClerkOAuthTokenResponse {

    private String token;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("public_metadata")
    private Object publicMetadata;

    @JsonProperty("label")
    private String label;

    @JsonProperty("scopes")
    private List<String> scopes;

    @JsonProperty("token_secret")
    private String tokenSecret;
}
