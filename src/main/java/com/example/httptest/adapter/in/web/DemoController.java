package com.example.httptest.adapter.in.web;

import com.example.httptest.adapter.out.http.client.DummyJsonClient;
import com.example.httptest.adapter.out.http.client.JsonPlaceholderClient;
import com.example.httptest.adapter.out.http.dto.PostDto;
import com.example.httptest.adapter.out.http.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 외부 HTTP 클라이언트를 호출하는 Inbound Web Adapter.
 *
 * <p>이 컨트롤러는 {@code @HttpExchange} 인터페이스만 주입받으며,
 * 내부적으로 RestClient / WebClient 중 어떤 엔진이 사용되는지 알지 못한다.
 * 엔진 교체 시 이 클래스는 수정할 필요가 없다.
 *
 * <h3>헥사고날 아키텍처 관점</h3>
 * <pre>
 * [클라이언트] → DemoController (Inbound Adapter)
 *                    ↓ 인터페이스 의존
 *              JsonPlaceholderClient / DummyJsonClient (Port = 순수 스펙)
 *                    ↓ Spring 프록시가 실제 호출 수행
 *              RestClient or WebClient (Outbound Adapter = 엔진)
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class DemoController {

    private final JsonPlaceholderClient jsonPlaceholderClient;
    private final DummyJsonClient dummyJsonClient;

    // ── JSONPlaceholder (posts) ──────────────────────────────

    @GetMapping("/posts")
    public List<PostDto> getAllPosts() {
        return jsonPlaceholderClient.getAll();
    }

    @GetMapping("/posts/{id}")
    public PostDto getPost(@PathVariable Long id) {
        return jsonPlaceholderClient.getById(id);
    }

    // ── DummyJSON (products) ─────────────────────────────────

    @GetMapping("/products/{id}")
    public ProductDto getProduct(@PathVariable Long id) {
        return dummyJsonClient.getById(id);
    }
}
