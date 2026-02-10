package com.example.httptest.adapter.out.http.client;

import com.example.httptest.adapter.out.http.dto.PostDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * JSONPlaceholder 외부 API를 선언적으로 정의하는 HTTP 인터페이스.
 *
 * <h3>핵심 포인트</h3>
 * <ul>
 *   <li>이 인터페이스는 순수한 "스펙(Specification)"이다.</li>
 *   <li>RestClient / WebClient 등 엔진에 대한 의존이 전혀 없다.</li>
 *   <li>엔진 선택은 {@code HttpClientConfig}에서 결정되며,
 *       이 인터페이스는 어떤 엔진이든 그대로 사용 가능하다. (Adapter Pattern)</li>
 * </ul>
 *
 * <h3>어노테이션 설명</h3>
 * <ul>
 *   <li>{@code @HttpExchange("/posts")} — 이 인터페이스의 모든 메서드에 공통 경로 prefix를 지정.
 *       base URL은 여기에 포함하지 않으며, 설정(Configurer 또는 application.yml)에서 주입된다.</li>
 *   <li>{@code @GetExchange} — HTTP GET 요청을 선언. Spring이 런타임에 프록시를 생성하여 실제 호출을 수행한다.</li>
 * </ul>
 */
@HttpExchange("/posts")
public interface JsonPlaceholderClient {

    @GetExchange
    List<PostDto> getAll();

    @GetExchange("/{id}")
    PostDto getById(@PathVariable("id") Long id);
}
