package com.example.httptest.adapter.out.http.config;

import com.example.httptest.adapter.out.http.client.DummyJsonClient;
import com.example.httptest.adapter.out.http.client.JsonPlaceholderClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Spring 7 선언적 HTTP 클라이언트의 어댑터 설정 클래스.
 *
 * <h3>헥사고날 아키텍처에서의 역할</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │  Interface (@HttpExchange)  =  Port (스펙, 엔진 무관)      │
 * │  이 Config 클래스            =  Adapter (엔진 바인딩)       │
 * │  Controller                 =  Inbound Adapter            │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>{@code @ImportHttpServices} 역할</h3>
 * <ul>
 *   <li><b>group</b> — 논리적 서비스 그룹 이름. 같은 그룹의 클라이언트는
 *       동일한 RestClient(또는 WebClient) 인스턴스를 공유한다.</li>
 *   <li><b>types</b> — 이 그룹에 포함할 {@code @HttpExchange} 인터페이스 목록.
 *       Spring이 각 인터페이스의 프록시 객체를 자동 생성하여 Bean으로 등록한다.</li>
 *   <li><b>clientType</b> — 엔진 선택. {@code REST_CLIENT} 또는 {@code WEB_CLIENT}.
 *       생략 시 기본값은 {@code REST_CLIENT}.</li>
 * </ul>
 *
 * <p>기존 Spring 6에서 수동으로 작성하던 {@code HttpServiceProxyFactory} +
 * {@code RestClient.builder()} 보일러플레이트가 이 어노테이션 하나로 대체된다.
 *
 * <h3>엔진 전환 방법 (RestClient → WebClient)</h3>
 * <ol>
 *   <li>{@code clientType}을 {@code HttpServiceGroup.ClientType.WEB_CLIENT}로 변경</li>
 *   <li>아래 Bean 타입을 {@code WebClientHttpServiceGroupConfigurer}로 교체</li>
 *   <li>빌더 타입이 {@code RestClient.Builder} → {@code WebClient.Builder}로 변경됨</li>
 *   <li><b>Interface, Controller 는 수정 불필요</b> — 이것이 Adapter Pattern의 핵심</li>
 * </ol>
 */
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
public class HttpClientConfig {

    /**
     * 각 HTTP 서비스 그룹의 RestClient를 커스터마이징하는 Configurer.
     *
     * <p>{@code groups.filterByName("그룹명")}으로 특정 그룹만 필터링하고,
     * {@code forEachClient}로 해당 그룹의 {@code RestClient.Builder}를 설정한다.
     *
     * <p>base URL은 {@code application.yml}의
     * {@code spring.http.serviceclient.<group>.base-url}로도 설정 가능하지만,
     * Configurer에서 직접 설정하면 추가 헤더·인터셉터 등 세밀한 제어가 가능하다.
     *
     * <p><b>WebClient 전환 시:</b> 이 메서드의 반환 타입을
     * {@code WebClientHttpServiceGroupConfigurer}로 바꾸고,
     * {@code clientBuilder} 파라미터가 {@code WebClient.Builder}로 변경된다.
     */
    @Bean
    RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer() {
        return groups -> {
            groups.filterByName("jsonplaceholder")
                    .forEachClient((group, clientBuilder) ->
                            clientBuilder
                                    .baseUrl("https://jsonplaceholder.typicode.com")
                                    .defaultHeader("Accept", "application/json")
                    );

            groups.filterByName("dummyjson")
                    .forEachClient((group, clientBuilder) ->
                            clientBuilder
                                    .baseUrl("https://dummyjson.com")
                                    .defaultHeader("Accept", "application/json")
                    );
        };
    }
}
