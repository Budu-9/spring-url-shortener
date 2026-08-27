package com.example.url_shortener.domain.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class BucketInfo {
    private long availableTokens;
    private long consumedTokens;
}
