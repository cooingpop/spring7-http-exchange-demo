# Spring 7 & Boot 4 Declarative HTTP Client Demo

Spring Framework 7 / Spring Boot 4.0 에서 새롭게 도입된 **선언적 HTTP 클라이언트** 기능을 데모하는 프로젝트입니다.

## 주요 특징

- **Zero-Config Bean Registration** — `@ImportHttpServices`로 수동 ProxyFactory/Bean 설정 없이 HTTP 클라이언트 자동 등록
- **Declarative HTTP Interface** — `@HttpExchange` 기반 인터페이스 선언만으로 REST API 호출
- **Hybrid Engine** — RestClient(동기) + WebClient(비동기) 한 프로젝트에서 공존
- **Multi-Service Groups** — 서비스별 독립적인 base URL, timeout, 헤더, 필터 설정
- **YML + Java DSL Hybrid** — 변하는 데이터는 YML에, 변하지 않는 로직은 Java에

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 4.0.2 |
| Spring Framework | 7.0.x |
| Gradle | 9.x |
| Tomcat | 11.x |

## 프로젝트 구조 (Hexagonal Architecture)

```
src/main/java/com/example/httptest/
├── HttpTestApplication.java
├── adapter/
│   ├── in/web/
│   │   └── DemoController.java                    # Inbound Adapter
│   └── out/http/
│       ├── client/
│       │   ├── JsonPlaceholderClient.java          # 동기 — RestClient
│       │   ├── DummyJsonClient.java                # 동기 — RestClient (다른 base URL)
│       │   └── JsonPlaceholderAsyncClient.java     # 비동기 — WebClient (Mono/Flux)
│       ├── config/
│       │   └── HttpClientConfig.java               # 엔진 바인딩 + 그룹 설정
│       └── dto/
│           ├── PostDto.java
│           ├── ProductDto.java
│           └── CommentDto.java
```

---

## 이 설정의 진짜 가치

> "엔진(RestClient/WebClient) 교체가 쉬워진다"는 이론적 이점이지만,
> 실무에서 이 구조가 주는 **진짜 가치**는 다른 곳에 있다.

### 1. 외부 API 스펙의 선언적 명세

```java
@HttpExchange("/posts")
public interface JsonPlaceholderClient {
    @GetExchange("/{id}")
    PostDto getById(@PathVariable("id") Long id);
}
```

- 인터페이스만 보면 **어떤 API를 어떻게 호출하는지** 즉시 파악 가능
- HTTP 라이브러리 코드(RestTemplate, WebClient.create() 등)가 비즈니스 로직에 섞이지 않음
- 외부 API 문서와 1:1 대응되는 코드 = **살아있는 API 명세서**

### 2. 설정 중앙화 — 10개 외부 API도 한눈에

```yaml
spring:
  http:
    serviceclient:
      jsonplaceholder:
        base-url: https://jsonplaceholder.typicode.com
      dummyjson:
        base-url: https://dummyjson.com
      async-comments:
        base-url: https://jsonplaceholder.typicode.com
```

- URL, timeout, 인증 정보가 **한 파일에 모여 있음**
- 프로파일(dev/staging/prod)별 전환 시 **Java 코드 수정 없이 yml만 변경**
- 서비스가 10개, 20개로 늘어나도 같은 패턴 반복

### 3. 테스트 용이성

```java
// 외부 API 호출 없이 단위 테스트 가능
@MockitoBean
private JsonPlaceholderClient mockClient;

@Test
void testGetPost() {
    given(mockClient.getById(1L)).willReturn(new PostDto(1L, 1L, "title", "body"));
    // ...
}
```

- `@HttpExchange` 인터페이스를 Mock하면 **외부 네트워크 없이** 테스트 가능
- 통합 테스트에서는 WireMock + yml의 base-url만 교체하면 됨

### 4. 그룹별 독립 설정 (필터, 인증, timeout)

```java
// WebClient 그룹에만 로깅 필터 적용
groups.filterByName("async-comments")
    .forEachClient((group, builder) ->
        builder.filter(logRequestFilter())
               .filter(logResponseFilter()));
```

- 특정 외부 API에만 **OAuth2 인증**, **로깅 필터**, **재시도 정책** 등을 적용
- 다른 그룹에 영향 없음

### 5. Spring 6 대비 보일러플레이트 제거

| Spring 6 (기존) | Spring 7 (신규) |
|-----------------|-----------------|
| `RestClient.builder().baseUrl(...)` | `application.yml` 한 줄 |
| `HttpServiceProxyFactory.builderFor(adapter).build()` | `@ImportHttpServices` 한 줄 |
| `proxyFactory.createClient(MyClient.class)` | 자동 Bean 등록 |
| 서비스마다 `@Bean` 메서드 반복 | `types = {A.class, B.class}` |

서비스 5개만 되어도 기존 방식은 **50줄 이상의 Config 코드**가 필요했으나,
Spring 7에서는 **어노테이션 5줄 + yml 10줄**로 끝남.

### 6. 엔진 교체에 대한 현실적 판단

| 상황 | 권장 |
|------|------|
| Spring MVC 프로젝트 | **RestClient 통일** (동기) |
| Spring WebFlux 프로젝트 | **WebClient 통일** (비동기) |
| 대부분 동기 + 일부 고지연 API | **하이브리드** (이 프로젝트처럼) |

> 엔진 교체 자체는 드문 일이다.
> 하지만 **"교체할 수 있는 구조"로 설계하면, 교체하지 않더라도 코드가 깔끔해진다.**
> 이것이 Adapter Pattern의 본질이다.

---

## 그룹 구성

| 그룹 | 엔진 | 방식 | 외부 API |
|------|------|------|----------|
| `jsonplaceholder` | RestClient | 동기 | jsonplaceholder.typicode.com/posts |
| `dummyjson` | RestClient | 동기 | dummyjson.com/products |
| `async-comments` | WebClient | 비동기 | jsonplaceholder.typicode.com/comments |

## 설정 전략: YML + Java DSL

| 설정 위치 | 역할 | 예시 |
|-----------|------|------|
| `application.yml` | 변하는 데이터 | base-url, read-timeout |
| Java Configurer | 변하지 않는 로직 | 로깅 필터, OAuth2, 커스텀 헤더 |
| `@Value` | 둘을 연결 | YML 값 → Java Configurer 참조 |

## 실행 방법

```bash
./gradlew bootRun
```

## API 테스트

```bash
# ── 동기 (RestClient) ────────────────────────
curl http://localhost:8080/posts/1          # JSONPlaceholder 포스트
curl http://localhost:8080/posts            # 전체 포스트
curl http://localhost:8080/products/1       # DummyJSON 상품

# ── 비동기 (WebClient) ───────────────────────
curl http://localhost:8080/comments/1       # 비동기 댓글 (로깅 필터 동작)
curl http://localhost:8080/comments         # 전체 댓글 (Flux 스트림)
```

### 응답 예시

**GET /posts/1** (동기/RestClient)
```json
{
  "id": 1, "userId": 1,
  "title": "sunt aut facere repellat provident...",
  "body": "quia et suscipit..."
}
```

**GET /products/1** (동기/RestClient)
```json
{
  "id": 1, "title": "Essence Mascara Lash Princess",
  "price": 9.99, "brand": "Essence", "category": "beauty"
}
```

**GET /comments/1** (비동기/WebClient — 서버 로그에 필터 출력)
```json
{
  "id": 1, "postId": 1,
  "name": "id labore ex et quam laborum",
  "email": "Eliseo@gardner.biz",
  "body": "laudantium enim quasi est quidem..."
}
```

```
# 서버 로그 출력
[WebClient Request] GET https://jsonplaceholder.typicode.com/comments/1
[WebClient Response] Status: 200 OK
```

## Spring 6 vs Spring 7 비교

| 항목 | Spring 6 (기존) | Spring 7 (신규) |
|------|-----------------|-----------------|
| 클라이언트 등록 | `HttpServiceProxyFactory` 수동 설정 | `@ImportHttpServices` 자동 등록 |
| Base URL 설정 | `RestClient.builder().baseUrl(...)` | YML + Configurer |
| 그룹 관리 | 불가 (개별 빈 설정) | `group` 속성으로 논리적 그룹화 |
| 멀티 서비스 | 서비스마다 Bean 메서드 | `@ImportHttpServices` 반복 선언 |
| 동기/비동기 공존 | 완전히 다른 설정 코드 | `clientType` 속성만 변경 |
| 필터/인터셉터 | 각 Bean에 개별 적용 | Configurer에서 그룹별 적용 |

## 참고 자료

- [HTTP Service Client Enhancements - Spring Blog](https://spring.io/blog/2025/09/23/http-service-client-enhancements/)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)
- [ImportHttpServices Javadoc](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/web/service/registry/ImportHttpServices.html)
- [RestClientHttpServiceGroupConfigurer Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/support/RestClientHttpServiceGroupConfigurer.html)
