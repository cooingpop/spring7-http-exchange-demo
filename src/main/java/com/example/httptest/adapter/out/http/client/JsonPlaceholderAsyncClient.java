package com.example.httptest.adapter.out.http.client;

import com.example.httptest.adapter.out.http.dto.CommentDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * JSONPlaceholder Comments API — <b>비동기(WebClient)</b> 선언적 HTTP 인터페이스.
 *
 * <h3>동기 vs 비동기 인터페이스 차이</h3>
 * <pre>
 * 동기 (RestClient):  PostDto getById(Long id)         → 블로킹, 값 직접 반환
 * 비동기 (WebClient): Mono&lt;CommentDto&gt; getById(Long id) → 논블로킹, 리액티브 타입 반환
 * </pre>
 *
 * <p>반환 타입만 다를 뿐, {@code @HttpExchange} 선언 방식은 동일하다.
 * 엔진 선택은 {@code HttpClientConfig}의 {@code clientType = WEB_CLIENT}에서 결정되며,
 * 이 인터페이스 자체에는 WebClient 코드가 전혀 없다.
 *
 * <h3>리액티브 반환 타입</h3>
 * <ul>
 *   <li>{@code Mono<T>} — 단건 비동기 응답</li>
 *   <li>{@code Flux<T>} — 다건 비동기 스트림</li>
 * </ul>
 */
@HttpExchange("/comments")
public interface JsonPlaceholderAsyncClient {

    @GetExchange("/{id}")
    Mono<CommentDto> getById(@PathVariable("id") Long id);

    @GetExchange
    Flux<CommentDto> getAll();
}
