# Spring 7의 선언적 HTTP 클라이언트, 왜 써야 하는가

> Spring Framework 7 / Spring Boot 4.0에서 도입된 `@ImportHttpServices`와 선언적 HTTP 클라이언트.
> "편해졌다"는 건 알겠는데, **실무에서 정말 의미가 있는 건가?**
> 직접 구현하고 테스트하면서 느낀 **진짜 가치**를 정리합니다.

---

## 1. 기존 방식: 우리가 매번 반복하던 코드

Spring 6에서 외부 API를 호출하려면 이런 코드를 서비스마다 작성해야 했다.

```java
// Spring 6 — 서비스마다 반복되는 보일러플레이트
@Configuration
public class JsonPlaceholderConfig {

    @Bean
    public JsonPlaceholderClient jsonPlaceholderClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com")
                .defaultHeader("Accept", "application/json")
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        return HttpServiceProxyFactory
                .builderFor(adapter)
                .build()
                .createClient(JsonPlaceholderClient.class);
    }
}
```

외부 API가 하나면 괜찮다. 하지만 실무에서는?

- 결제 API, 알림 API, 인증 API, 검색 API, 배송 API...
- **서비스마다 이 코드를 복붙**하고, URL만 바꾸고, 빈 이름만 바꾼다.
- 5개면 50줄, 10개면 100줄의 **거의 동일한 Config 코드**가 쌓인다.

문제는 코드 양만이 아니다.

- URL이 Config 클래스 안에 하드코딩되거나 흩어져 있다.
- 어떤 서비스가 어떤 클라이언트를 쓰는지 한눈에 파악이 안 된다.
- 테스트할 때 Mock 설정도 서비스마다 달라진다.

---

## 2. Spring 7이 바꾼 것: 어노테이션 하나

```java
// Spring 7 — 이게 전부다
@Configuration
@ImportHttpServices(group = "jsonplaceholder", types = JsonPlaceholderClient.class)
@ImportHttpServices(group = "dummyjson", types = DummyJsonClient.class)
public class HttpClientConfig {
}
```

`@ImportHttpServices` 하나로:
- **프록시 생성** — Spring이 인터페이스의 구현체를 자동 생성
- **Bean 등록** — `@Autowired`로 바로 주입 가능
- **그룹화** — 같은 그룹의 클라이언트는 동일한 HTTP 엔진 인스턴스를 공유

URL은 `application.yml`로 빠진다:

```yaml
spring:
  http:
    serviceclient:
      jsonplaceholder:
        base-url: https://jsonplaceholder.typicode.com
        read-timeout: 5s
      dummyjson:
        base-url: https://dummyjson.com
        read-timeout: 5s
```

**서비스가 10개로 늘어나도 같은 패턴의 반복**이다. 보일러플레이트가 아니라 선언이다.

---

## 3. "엔진 교체가 쉽다"는 말의 진실

Spring 7 소개 글을 보면 항상 이런 문구가 나온다:

> "RestClient에서 WebClient로 Config만 바꾸면 전환 가능!"

**반은 맞고 반은 틀리다.**

### 맞는 부분

`@HttpExchange` 인터페이스에는 엔진 코드가 전혀 없다:

```java
@HttpExchange("/posts")
public interface JsonPlaceholderClient {
    @GetExchange("/{id}")
    PostDto getById(@PathVariable("id") Long id);
}
```

이 인터페이스는 RestClient든 WebClient든 상관없이 동작한다.
엔진 결정은 Config의 `clientType` 한 줄이다:

```java
@ImportHttpServices(
    group = "jsonplaceholder",
    types = JsonPlaceholderClient.class,
    clientType = HttpServiceGroup.ClientType.REST_CLIENT  // ← 여기만 바꾸면?
)
```

### 틀리는 부분

**동기 → 비동기 전환은 "Config만 바꾸면 끝"이 아니다.**

```java
// 동기 (RestClient)
PostDto getById(Long id);

// 비동기 (WebClient)
Mono<PostDto> getById(Long id);
```

반환 타입이 `PostDto` → `Mono<PostDto>`로 바뀌는 순간,
인터페이스, 컨트롤러, 서비스 계층, 테스트 코드 **전부 수정**이 필요하다.

### 현실적인 결론

| 상황 | 교체 범위 |
|------|-----------|
| 동기 → 동기 (HTTP 라이브러리만 변경) | Config 1개 파일 |
| 동기 → 비동기 (반환 타입 변경) | 인터페이스 + 호출자 전부 |

엔진 교체는 **드문 일**이다. 대부분의 프로젝트는 하나를 선택하고 유지한다.

---

## 4. 그러면 이 구조의 진짜 가치는 뭔가

엔진 교체가 아니라, **관심사 분리(Separation of Concerns)** 에서 온다.

### 가치 1: 외부 API가 "코드로 문서화"된다

```java
@HttpExchange("/posts")
public interface JsonPlaceholderClient {

    @GetExchange
    List<PostDto> getAll();

    @GetExchange("/{id}")
    PostDto getById(@PathVariable("id") Long id);
}
```

이 인터페이스만 보면:
- 어떤 경로를 호출하는지 (`/posts`, `/posts/{id}`)
- 어떤 HTTP 메서드인지 (`GET`)
- 응답 형태가 뭔지 (`PostDto`, `List<PostDto>`)

**별도의 API 문서 없이도** 외부 연동 스펙이 파악된다.

RestTemplate 시절에는 이런 코드가 서비스 계층 곳곳에 흩어져 있었다:

```java
// 이전 방식 — 어디서 뭘 호출하는지 찾으려면 전체 검색이 필요
ResponseEntity<PostDto> response = restTemplate.getForEntity(
    "https://jsonplaceholder.typicode.com/posts/" + id, PostDto.class);
```

### 가치 2: 설정이 한 곳에 모인다

실무에서 외부 API 연동 시 가장 흔한 장애 원인:

- "dev 환경 URL이 prod로 배포됐다"
- "timeout이 너무 짧게 설정되어 있었다"
- "인증 헤더가 빠졌다"

Spring 7에서는 **모든 외부 연동 설정이 두 곳에만 존재**한다:

```
application.yml          → URL, timeout (환경별로 변하는 값)
HttpClientConfig.java    → 필터, 인증, 헤더 (로직)
```

서비스가 10개든 20개든, 설정을 찾으려면 이 두 파일만 보면 된다.

### 가치 3: 테스트가 단순해진다

```java
@SpringBootTest
class DemoControllerTest {

    @MockitoBean
    private JsonPlaceholderClient mockClient;

    @Test
    void testGetPost() {
        given(mockClient.getById(1L))
            .willReturn(new PostDto(1L, 1L, "title", "body"));

        // 외부 네트워크 호출 없이 테스트 완료
    }
}
```

인터페이스를 Mock하면 **외부 API 서버 없이** 단위 테스트가 가능하다.

통합 테스트에서는 WireMock + yml의 `base-url`만 교체하면 된다:

```yaml
# application-test.yml
spring:
  http:
    serviceclient:
      jsonplaceholder:
        base-url: http://localhost:${wiremock.server.port}
```

### 가치 4: 그룹별로 다른 정책을 적용할 수 있다

결제 API에는 재시도 + 짧은 timeout, 로그 API에는 긴 timeout + 로깅 필터.
이런 **서비스별 차등 정책**이 그룹 단위로 깔끔하게 분리된다:

```java
@Bean
RestClientHttpServiceGroupConfigurer restConfigurer() {
    return groups -> {
        groups.filterByName("payment")
              .forEachClient((group, builder) ->
                  builder.baseUrl(paymentUrl)
                         .defaultHeader("Authorization", "Bearer " + token));

        groups.filterByName("logging")
              .forEachClient((group, builder) ->
                  builder.baseUrl(loggingUrl));
    };
}
```

다른 그룹에 영향을 주지 않고, 특정 서비스에만 OAuth2, 로깅 필터, 커스텀 직렬화 등을 적용할 수 있다.

---

## 5. 하이브리드 구성: 동기 + 비동기 공존

대부분의 외부 API는 동기(RestClient)로 충분하다.
하지만 **응답이 느린 API가 일부 있다면?** 그 API만 비동기(WebClient)로 처리할 수 있다.

```java
@Configuration
// 동기 그룹
@ImportHttpServices(group = "jsonplaceholder", types = JsonPlaceholderClient.class,
                    clientType = HttpServiceGroup.ClientType.REST_CLIENT)
// 비동기 그룹
@ImportHttpServices(group = "async-comments", types = JsonPlaceholderAsyncClient.class,
                    clientType = HttpServiceGroup.ClientType.WEB_CLIENT)
public class HttpClientConfig {

    // 동기 그룹 설정
    @Bean
    RestClientHttpServiceGroupConfigurer restConfigurer(...) { ... }

    // 비동기 그룹 설정 + 로깅 필터
    @Bean
    WebClientHttpServiceGroupConfigurer webClientConfigurer(...) {
        return groups -> groups
            .filterByName("async-comments")
            .forEachClient((group, builder) ->
                builder.baseUrl(asyncBaseUrl)
                       .filter(logRequestFilter())    // WebClient 전용 필터
                       .filter(logResponseFilter()));
    }
}
```

비동기 인터페이스는 반환 타입만 다르다:

```java
@HttpExchange("/comments")
public interface JsonPlaceholderAsyncClient {

    @GetExchange("/{id}")
    Mono<CommentDto> getById(@PathVariable("id") Long id);  // 단건 비동기

    @GetExchange
    Flux<CommentDto> getAll();                                // 다건 스트림
}
```

컨트롤러에서는 동기/비동기를 **자연스럽게 섞어서 사용**할 수 있다:

```java
@RestController
@RequiredArgsConstructor
public class DemoController {

    private final JsonPlaceholderClient syncClient;      // RestClient
    private final JsonPlaceholderAsyncClient asyncClient; // WebClient

    @GetMapping("/posts/{id}")
    public PostDto getPost(@PathVariable Long id) {
        return syncClient.getById(id);                    // 동기 반환
    }

    @GetMapping("/comments/{id}")
    public Mono<CommentDto> getComment(@PathVariable Long id) {
        return asyncClient.getById(id);                   // 비동기 반환
    }
}
```

---

## 6. 설정 전략: YML과 Java, 어디에 뭘 넣을까

Spring 7은 YML과 Java DSL 두 가지 설정 방식을 모두 지원한다.
하지만 **아무 데나 넣으면 안 된다.** 원칙이 있다:

> **변하는 데이터는 YML에, 변하지 않는 로직은 Java에.**

| 구분 | YML (`application.yml`) | Java (Configurer) |
|------|------------------------|-------------------|
| URL | `base-url: https://api.example.com` | - |
| Timeout | `read-timeout: 5s` | - |
| 로깅 필터 | - | `builder.filter(logRequest())` |
| OAuth2 인증 | - | `builder.filter(oauth2Filter())` |
| 커스텀 직렬화 | - | `builder.codecs(...)` |
| 재시도 정책 | - | `builder.filter(retryFilter())` |

Java Configurer에서 YML 값을 참조하려면 `@Value`를 사용한다:

```java
@Bean
RestClientHttpServiceGroupConfigurer configurer(
        @Value("${spring.http.serviceclient.payment.base-url}") String paymentUrl) {
    return groups -> groups
        .filterByName("payment")
        .forEachClient((group, builder) -> builder.baseUrl(paymentUrl));
}
```

이렇게 하면:
- **dev/staging/prod 환경 전환** 시 yml 프로파일만 교체
- **필터 로직 변경** 시 Java 코드만 수정
- 두 관심사가 섞이지 않음

---

## 7. Spring 6 vs Spring 7: 코드로 비교

### 외부 API 3개를 연동하는 상황

**Spring 6 (기존)**

```java
@Configuration
public class HttpClientConfig {

    @Bean
    public PaymentClient paymentClient() {
        RestClient restClient = RestClient.builder()
            .baseUrl("https://api.payment.com")
            .defaultHeader("Authorization", "Bearer xxx")
            .build();
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(PaymentClient.class);
    }

    @Bean
    public NotificationClient notificationClient() {
        RestClient restClient = RestClient.builder()
            .baseUrl("https://api.notification.com")
            .build();
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(NotificationClient.class);
    }

    @Bean
    public SearchClient searchClient() {
        RestClient restClient = RestClient.builder()
            .baseUrl("https://api.search.com")
            .build();
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(SearchClient.class);
    }
}
// → 약 40줄, 패턴이 동일한 코드 3번 반복
```

**Spring 7 (신규)**

```java
@Configuration
@ImportHttpServices(group = "payment", types = PaymentClient.class)
@ImportHttpServices(group = "notification", types = NotificationClient.class)
@ImportHttpServices(group = "search", types = SearchClient.class)
public class HttpClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer configurer() {
        return groups -> groups.forEachClient((group, builder) ->
            builder.defaultHeader("Accept", "application/json"));
    }
}
// → 약 12줄, URL은 yml에
```

```yaml
spring:
  http:
    serviceclient:
      payment:
        base-url: https://api.payment.com
      notification:
        base-url: https://api.notification.com
      search:
        base-url: https://api.search.com
```

**40줄 → 12줄 + yml 8줄.** 서비스가 늘어날수록 차이는 더 벌어진다.

---

## 8. 정리

Spring 7의 선언적 HTTP 클라이언트가 주는 가치를 한 문장으로 요약하면:

> **"외부 API 연동 코드에서 인프라 관심사를 완전히 분리하여,
> 인터페이스는 스펙만, 설정은 설정만, 비즈니스는 비즈니스만 담당하게 한다."**

| 가치 | 설명 |
|------|------|
| 선언적 명세 | `@HttpExchange` 인터페이스 = 살아있는 API 문서 |
| 설정 중앙화 | URL, timeout, 인증이 yml + Config 두 곳에만 존재 |
| 테스트 용이성 | 인터페이스 Mock만으로 외부 의존성 제거 |
| 그룹별 정책 | 서비스마다 다른 필터, 인증, timeout 적용 |
| 보일러플레이트 제거 | 서비스 N개 × 10줄 → 어노테이션 N줄 + yml N줄 |

"엔진 교체가 쉽다"는 부수적 이점이다.
**교체하지 않더라도, 이 구조로 설계하면 코드가 깔끔해진다.**
그것이 Adapter Pattern의 본질이고, Spring 7이 프레임워크 수준에서 이를 지원하게 된 것이다.

---

## 참고 자료

- [HTTP Service Client Enhancements - Spring Blog](https://spring.io/blog/2025/09/23/http-service-client-enhancements/)
- [The State of HTTP Clients in Spring](https://spring.io/blog/2025/09/30/the-state-of-http-clients-in-spring/)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)
- [ImportHttpServices Javadoc](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/web/service/registry/ImportHttpServices.html)
- [RestClientHttpServiceGroupConfigurer Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/support/RestClientHttpServiceGroupConfigurer.html)
- [데모 프로젝트 GitHub](https://github.com/cooingpop/spring7-http-exchange-demo)
