package com.wallet.personalization.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalizationBrandRow {
    private Long categoryId;
    private String brandName;
    private Integer priority;
}
