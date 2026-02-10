package com.example.httptest.adapter.in.web;

import com.example.httptest.adapter.out.http.client.DummyJsonClient;
import com.example.httptest.adapter.out.http.client.JsonPlaceholderAsyncClient;
import com.example.httptest.adapter.out.http.client.JsonPlaceholderClient;
import com.example.httptest.adapter.out.http.dto.CommentDto;
import com.example.httptest.adapter.out.http.dto.PostDto;
import com.example.httptest.adapter.out.http.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 동기 + 비동기 HTTP 클라이언트를 모두 호출하는 Inbound Web Adapter.
 *
 * <p>이 컨트롤러는 3개의 {@code @HttpExchange} 인터페이스를 주입받지만,
 * 내부적으로 어떤 엔진(RestClient/WebClient)이 사용되는지 전혀 알지 못한다.
 *
 * <h3>엔드포인트 구성</h3>
 * <pre>
 * ┌────────────────────┬────────────┬────────────────────────────────┐
 * │ 엔드포인트           │ 방식        │ 외부 API                       │
 * ├────────────────────┼────────────┼────────────────────────────────┤
 * │ GET /posts         │ 동기        │ jsonplaceholder.typicode.com   │
 * │ GET /posts/{id}    │ 동기        │ jsonplaceholder.typicode.com   │
 * │ GET /products/{id} │ 동기        │ dummyjson.com                  │
 * │ GET /comments/{id} │ 비동기      │ jsonplaceholder.typicode.com   │
 * │ GET /comments      │ 비동기      │ jsonplaceholder.typicode.com   │
 * └────────────────────┴────────────┴────────────────────────────────┘
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class DemoController {

    private final JsonPlaceholderClient jsonPlaceholderClient;
    private final DummyJsonClient dummyJsonClient;
    private final JsonPlaceholderAsyncClient asyncCommentClient;

    // ── 동기: JSONPlaceholder Posts (RestClient) ─────────────

    @GetMapping("/posts")
    public List<PostDto> getAllPosts() {
        return jsonPlaceholderClient.getAll();
    }

    @GetMapping("/posts/{id}")
    public PostDto getPost(@PathVariable Long id) {
        return jsonPlaceholderClient.getById(id);
    }

    // ── 동기: DummyJSON Products (RestClient) ────────────────

    @GetMapping("/products/{id}")
    public ProductDto getProduct(@PathVariable Long id) {
        return dummyJsonClient.getById(id);
    }

    // ── 비동기: JSONPlaceholder Comments (WebClient) ─────────

    @GetMapping("/comments/{id}")
    public Mono<CommentDto> getComment(@PathVariable Long id) {
        return asyncCommentClient.getById(id);
    }

    @GetMapping("/comments")
    public Flux<CommentDto> getAllComments() {
        return asyncCommentClient.getAll();
    }
}
