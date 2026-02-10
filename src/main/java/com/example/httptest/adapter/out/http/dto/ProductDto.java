package com.example.httptest.adapter.out.http.dto;

/**
 * DummyJSON 외부 API 응답을 매핑하는 DTO.
 *
 * <p>JSONPlaceholder와는 완전히 다른 외부 서비스(base URL)이며,
 * 멀티 서비스 그룹 구성을 데모하기 위해 추가되었다.
 *
 * @see <a href="https://dummyjson.com/products/1">DummyJSON Products API</a>
 */
public record ProductDto(
        Long id,
        String title,
        String description,
        Double price,
        String brand,
        String category
) {
}
