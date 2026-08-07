package com.wallet.personalization.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.personalization.dto.PersonalizationBrandRequest;
import com.wallet.personalization.dto.PersonalizationGroupResponse;
import com.wallet.personalization.dto.PersonalizationMerchantResponse;
import com.wallet.personalization.dto.PersonalizationResponse;
import com.wallet.personalization.dto.PersonalizationSubcategoryResponse;
import com.wallet.personalization.dto.PersonalizationUpdateRequest;
import com.wallet.personalization.mapper.PersonalizationBrandRow;
import com.wallet.personalization.mapper.PersonalizationCatalogRow;
import com.wallet.personalization.mapper.PersonalizationMapper;

@RequiredArgsConstructor
@Service
public class PersonalizationService {
    private static final int MAX_BRANDS_PER_CATEGORY = 3;
    private static final int MAX_BRAND_NAME_LENGTH = 100;

    private final PersonalizationMapper personalizationMapper;

    @Transactional(readOnly = true)
    public PersonalizationResponse get(Long memberId) {
        validateMemberId(memberId);
        return load(memberId);
    }

    @Transactional
    public PersonalizationResponse update(Long memberId, PersonalizationUpdateRequest request) {
        validateMemberId(memberId);
        if (request == null || request.brands() == null) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }

        List<Long> categoryIds = distinctIds(request.categoryIds());
        Map<Long, String> categoryCodeById = loadCategoryCodes(memberId);
        validateCategories(categoryIds, categoryCodeById.keySet());
        Map<Long, List<String>> brandsByCategory = validateBrands(request.brands(), Set.copyOf(categoryIds));

        // 문자열 브랜드 테이블의 FK가 카테고리 행을 참조하므로 카테고리 삭제만으로 기존 브랜드도 함께 정리된다.
        personalizationMapper.deletePersonalizationCategories(memberId);
        for (Long categoryId : categoryIds) {
            String categoryKey = categoryCodeById.get(categoryId);
            personalizationMapper.insertPersonalizationCategory(memberId, categoryKey);
            List<String> brands = brandsByCategory.getOrDefault(categoryId, List.of());
            for (int index = 0; index < brands.size(); index++) {
                personalizationMapper.insertPersonalizationBrand(
                    memberId, categoryKey, index + 1, brands.get(index)
                );
            }
        }

        return load(memberId);
    }

    private PersonalizationResponse load(Long memberId) {
        Map<Long, List<String>> brandsByCategory = new HashMap<>();
        for (PersonalizationBrandRow brand : personalizationMapper.findPersonalizationBrands(memberId)) {
            brandsByCategory.computeIfAbsent(brand.getCategoryId(), ignored -> new ArrayList<>())
                .add(brand.getBrandName());
        }

        Map<Long, GroupBuilder> groups = new LinkedHashMap<>();
        for (PersonalizationCatalogRow row : personalizationMapper.findCatalog(memberId)) {
            GroupBuilder group = groups.computeIfAbsent(row.getParentCategoryId(), ignored ->
                new GroupBuilder(row.getParentCategoryId(), row.getParentCategoryCode(), row.getParentCategoryName())
            );
            SubcategoryBuilder child = group.children.computeIfAbsent(row.getCategoryId(), ignored ->
                new SubcategoryBuilder(
                    row.getCategoryId(), row.getCategoryCode(), row.getCategoryName(),
                    Boolean.TRUE.equals(row.getSelected()), brandsByCategory.getOrDefault(row.getCategoryId(), List.of())
                )
            );
            if (row.getMerchantId() != null) {
                child.merchants.add(new PersonalizationMerchantResponse(
                    row.getMerchantId(), row.getMerchantName(), false, null
                ));
            }
        }

        return new PersonalizationResponse(groups.values().stream().map(GroupBuilder::toResponse).toList());
    }

    private Map<Long, String> loadCategoryCodes(Long memberId) {
        Map<Long, String> result = new LinkedHashMap<>();
        for (PersonalizationCatalogRow row : personalizationMapper.findCatalog(memberId)) {
            result.putIfAbsent(row.getCategoryId(), row.getCategoryCode());
        }
        return result;
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
        LinkedHashSet<Long> distinct = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0 || !distinct.add(id)) {
                throw new BusinessException(ErrorCode.INPUT_INVALID);
            }
        }
        return List.copyOf(distinct);
    }

    private void validateCategories(List<Long> categoryIds, Set<Long> availableCategoryIds) {
        if (!availableCategoryIds.containsAll(categoryIds)) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
    }

    private Map<Long, List<String>> validateBrands(List<PersonalizationBrandRequest> requests,
                                                    Set<Long> selectedCategoryIds) {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        Map<Long, Set<String>> normalizedNames = new HashMap<>();
        for (PersonalizationBrandRequest request : requests) {
            if (request == null || request.categoryId() == null
                || !selectedCategoryIds.contains(request.categoryId()) || request.brandName() == null) {
                throw new BusinessException(ErrorCode.INPUT_INVALID);
            }
            String brandName = request.brandName().trim();
            if (brandName.isEmpty() || brandName.length() > MAX_BRAND_NAME_LENGTH) {
                throw new BusinessException(ErrorCode.INPUT_INVALID);
            }
            List<String> categoryBrands = result.computeIfAbsent(request.categoryId(), ignored -> new ArrayList<>());
            Set<String> names = normalizedNames.computeIfAbsent(request.categoryId(), ignored -> new LinkedHashSet<>());
            if (categoryBrands.size() >= MAX_BRANDS_PER_CATEGORY
                || !names.add(brandName.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.INPUT_INVALID);
            }
            categoryBrands.add(brandName);
        }
        return result;
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    private static class GroupBuilder {
        private final Long id;
        private final String code;
        private final String name;
        private final Map<Long, SubcategoryBuilder> children = new LinkedHashMap<>();

        private GroupBuilder(Long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        private PersonalizationGroupResponse toResponse() {
            return new PersonalizationGroupResponse(
                id, code, name, children.values().stream().map(SubcategoryBuilder::toResponse).toList()
            );
        }
    }

    private static class SubcategoryBuilder {
        private final Long id;
        private final String code;
        private final String name;
        private final boolean selected;
        private final List<String> brands;
        private final List<PersonalizationMerchantResponse> merchants = new ArrayList<>();

        private SubcategoryBuilder(Long id, String code, String name, boolean selected, List<String> brands) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.selected = selected;
            this.brands = List.copyOf(brands);
        }

        private PersonalizationSubcategoryResponse toResponse() {
            return new PersonalizationSubcategoryResponse(id, code, name, selected, brands, List.copyOf(merchants));
        }
    }
}
