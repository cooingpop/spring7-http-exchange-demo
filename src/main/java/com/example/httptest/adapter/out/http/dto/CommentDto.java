package com.example.httptest.adapter.out.http.dto;

/**
 * JSONPlaceholder Comments API 응답 DTO.
 *
 * <p>비동기(WebClient) 그룹 데모를 위해 사용되며,
 * {@code Mono<CommentDto>} / {@code Flux<CommentDto>} 형태로 반환된다.
 *
 * @see <a href="https://jsonplaceholder.typicode.com/comments/1">JSONPlaceholder Comments API</a>
 */
public record CommentDto(
        Long id,
        Long postId,
        String name,
        String email,
        String body
) {
}
