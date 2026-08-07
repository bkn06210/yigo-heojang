package com.wallet.transaction.dto;

import java.util.List;

public class ExpenseCategoryListResponse {

    private List<ExpenseCategoryResponse> categories;

    public ExpenseCategoryListResponse(List<ExpenseCategoryResponse> categories) {
        this.categories = categories;
    }

    public List<ExpenseCategoryResponse> getCategories() {
        return categories;
    }

    public void setCategories(List<ExpenseCategoryResponse> categories) {
        this.categories = categories;
    }
}
