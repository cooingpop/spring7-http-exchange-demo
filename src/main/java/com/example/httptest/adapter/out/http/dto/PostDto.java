package com.example.httptest.adapter.out.http.dto;

/**
 * JSONPlaceholder 외부 API 응답을 매핑하는 DTO.
 *
 * <p>Java 21 record를 사용하여 불변 객체로 정의.
 * 외부 API의 JSON 필드명과 1:1 매핑된다.
 *
 * @see <a href="https://jsonplaceholder.typicode.com/posts/1">JSONPlaceholder Posts API</a>
 */
public record PostDto(
        Long id,
        Long userId,
        String title,
        String body
) {
}
