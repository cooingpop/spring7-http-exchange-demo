package com.example.httptest.adapter.out.http.config;

import com.example.httptest.adapter.out.http.client.DummyJsonClient;
import com.example.httptest.adapter.out.http.client.JsonPlaceholderAsyncClient;
import com.example.httptest.adapter.out.http.client.JsonPlaceholderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;
import reactor.core.publisher.Mono;

/**
 * Spring 7 하이브리드 HTTP 클라이언트 설정 — RestClient & WebClient 공존.
 *
 * <h3>그룹 구성</h3>
 * <pre>
 * ┌──────────────────┬────────────┬────────────────────────────────────────────┐
 * │ 그룹              │ 엔진        │ 용도                                       │
 * ├──────────────────┼────────────┼────────────────────────────────────────────┤
 * │ jsonplaceholder  │ RestClient │ 동기 — Posts API                            │
 * │ dummyjson        │ RestClient │ 동기 — Products API (다른 base URL)          │
 * │ async-comments   │ WebClient  │ 비동기 — Comments API (Mono/Flux 반환)       │
 * └──────────────────┴────────────┴────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>설정 전략: YML + Java DSL 하이브리드</h3>
 * <ul>
 *   <li><b>변하는 데이터</b> (URL, timeout) → {@code application.yml}에 정의</li>
 *   <li><b>변하지 않는 로직</b> (필터, 인터셉터) → Java Configurer에서 구현</li>
 * </ul>
 *
 * <p>{@code @Value}로 YML 값을 주입받아 Configurer에서 참조하는 구조이며,
 * 프로파일(dev/prod)별 URL 변경 시 Java 코드를 수정할 필요가 없다.
 *
 * <h3>{@code @ImportHttpServices} 역할</h3>
 * <ul>
 *   <li><b>group</b> — 논리적 서비스 그룹. 같은 그룹의 클라이언트는 동일한 HTTP 엔진 인스턴스를 공유</li>
 *   <li><b>types</b> — Bean으로 등록할 {@code @HttpExchange} 인터페이스 목록 (프록시 자동 생성)</li>
 *   <li><b>clientType</b> — {@code REST_CLIENT}(동기) 또는 {@code WEB_CLIENT}(비동기)</li>
 * </ul>
 */
@Slf4j
@Configuration
@ImportHttpServices(
        group = "jsonplaceholder",
        types = JsonPlaceholderClient.class,
        clientType = HttpServiceGroup.ClientType.REST_CLIENT
)
@ImportHttpServices(
        group = "dummyjson",
        types = DummyJsonClient.class,
        clientType = HttpServiceGroup.ClientType.REST_CLIENT
)
@ImportHttpServices(
        group = "async-comments",
        types = JsonPlaceholderAsyncClient.class,
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT
)
public class HttpClientConfig {

    // ──────────────────────────────────────────────────────────
    // 동기 그룹 (RestClient) — jsonplaceholder, dummyjson
    // ──────────────────────────────────────────────────────────

    /**
     * RestClient 그룹 Configurer.
     *
     * <p>{@code @Value}로 YML의 base-url을 주입받아 설정한다.
     * 헤더 등 공통 설정은 Java DSL에서 처리.
     */
    @Bean
    RestClientHttpServiceGroupConfigurer restClientGroupConfigurer(
            @Value("${spring.http.serviceclient.jsonplaceholder.base-url}") String jsonPlaceholderUrl,
            @Value("${spring.http.serviceclient.dummyjson.base-url}") String dummyJsonUrl) {

        return groups -> {
            groups.filterByName("jsonplaceholder")
                    .forEachClient((group, builder) ->
                            builder.baseUrl(jsonPlaceholderUrl)
                                    .defaultHeader("Accept", "application/json")
                    );

            groups.filterByName("dummyjson")
                    .forEachClient((group, builder) ->
                            builder.baseUrl(dummyJsonUrl)
                                    .defaultHeader("Accept", "application/json")
                    );
        };
    }

    // ──────────────────────────────────────────────────────────
    // 비동기 그룹 (WebClient) — async-comments
    // ──────────────────────────────────────────────────────────

    /**
     * WebClient 그룹 Configurer.
     *
     * <p>RestClient Configurer와 구조는 동일하지만:
     * <ul>
     *   <li>반환 타입: {@code WebClientHttpServiceGroupConfigurer}</li>
     *   <li>빌더 타입: {@code WebClient.Builder} (filter 메서드 사용 가능)</li>
     *   <li>{@code ExchangeFilterFunction}으로 요청/응답 로깅 필터 추가</li>
     * </ul>
     */
    @Bean
    WebClientHttpServiceGroupConfigurer webClientGroupConfigurer(
            @Value("${spring.http.serviceclient.async-comments.base-url}") String asyncBaseUrl) {

        return groups -> groups
                .filterByName("async-comments")
                .forEachClient((group, builder) ->
                        builder.baseUrl(asyncBaseUrl)
                                .defaultHeader("Accept", "application/json")
                                .filter(logRequestFilter())
                                .filter(logResponseFilter())
                );
    }

    /**
     * WebClient 요청 로깅 필터 — Java DSL 방식.
     *
     * <p>YML로는 불가능한 로직(필터, 인터셉터, OAuth2 등)을
     * Java Configurer에서 구현하는 대표적인 사례.
     */
    private static ExchangeFilterFunction logRequestFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("[WebClient Request] {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    /**
     * WebClient 응답 로깅 필터.
     */
    private static ExchangeFilterFunction logResponseFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("[WebClient Response] Status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}
