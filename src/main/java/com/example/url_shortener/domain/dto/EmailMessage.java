package com.example.url_shortener.domain.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record EmailMessage(
        String to,
        String subject,
        String htmlBody,
        String textBody,         // plain-text fallback (accessibility + spam filters)
        List<String> cc,         // optional
        List<String> bcc
) {}
