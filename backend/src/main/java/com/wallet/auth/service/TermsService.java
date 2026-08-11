package com.wallet.auth.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.dto.TermResponse;
import com.wallet.auth.dto.TermsResponse;
import com.wallet.auth.mapper.TermsMapper;

@RequiredArgsConstructor
@Service
public class TermsService {
    private final TermsMapper termsMapper;

    @Transactional(readOnly = true)
    public TermsResponse getTerms() {
        List<TermResponse> terms = termsMapper.findActiveTerms();

        return new TermsResponse(terms);
    }
}