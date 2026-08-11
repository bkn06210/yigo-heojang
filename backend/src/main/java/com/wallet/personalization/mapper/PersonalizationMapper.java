package com.wallet.personalization.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PersonalizationMapper {
    List<PersonalizationCatalogRow> findCatalog(@Param("memberId") Long memberId);

    List<PersonalizationBrandRow> findPersonalizationBrands(@Param("memberId") Long memberId);

    int deletePersonalizationCategories(@Param("memberId") Long memberId);

    int insertPersonalizationCategory(@Param("memberId") Long memberId,
                                      @Param("categoryKey") String categoryKey);

    int insertPersonalizationBrand(@Param("memberId") Long memberId,
                                   @Param("categoryKey") String categoryKey,
                                   @Param("priority") int priority,
                                   @Param("brandName") String brandName);
}
