package com.pickme.product.domain.model;

import java.util.Objects;

public class Category {

    private final String code;
    private final String name;

    public Category(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("카테고리 코드는 비어있을 수 없습니다");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 비어있을 수 없습니다");
        }
        this.code = code.strip();
        this.name = name.strip();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(code, category.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return String.format("Category{code='%s', name='%s'}", code, name);
    }
}
