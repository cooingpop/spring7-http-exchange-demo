# Spring 7 & Boot 4 Declarative HTTP Client Demo

Spring Framework 7 / Spring Boot 4.0 에서 새롭게 도입된 **선언적 HTTP 클라이언트** 기능을 데모하는 프로젝트입니다.

## 주요 특징

- **Zero-Config Bean Registration** — `@ImportHttpServices`로 수동 ProxyFactory/Bean 설정 없이 HTTP 클라이언트 자동 등록
- **Declarative HTTP Interface** — `@HttpExchange` 기반 인터페이스 선언만으로 REST API 호출
- **Adapter Pattern (Hexagonal)** — `RestClient` / `WebClient` 전환 시 Interface와 Controller 수정 불필요
- **Multi-Service Groups** — 서비스별 독립적인 base URL, timeout, 헤더 설정
- **Java 21 Virtual Threads** ready

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
├── HttpTestApplication.java                    # 메인 애플리케이션
├── adapter/
│   ├── in/web/
│   │   └── DemoController.java                 # Inbound Adapter (REST 컨트롤러)
│   └── out/http/
│       ├── client/
│       │   ├── JsonPlaceholderClient.java       # Port — @HttpExchange 스펙 (엔진 무관)
│       │   └── DummyJsonClient.java             # Port — 두 번째 외부 서비스
│       ├── config/
│       │   └── HttpClientConfig.java            # Adapter — 엔진 바인딩 + 그룹 설정
│       └── dto/
│           ├── PostDto.java                     # 외부 API 응답 DTO
│           └── ProductDto.java                  # 외부 API 응답 DTO
```

### 아키텍처 흐름

```
[클라이언트 요청]
    ↓
DemoController (Inbound Adapter)
    ↓ 인터페이스만 의존 (엔진 모름)
JsonPlaceholderClient / DummyJsonClient (Port = 순수 스펙)
    ↓ Spring 프록시가 실제 HTTP 호출 수행
RestClient 또는 WebClient (Outbound Adapter = 엔진)
    ↓
[외부 API 서버]
```

## 핵심 코드 설명

### 1. 선언적 HTTP 인터페이스 — Port (엔진 무관)

```java
@HttpExchange("/posts")
public interface JsonPlaceholderClient {

    @GetExchange
    List<PostDto> getAll();

    @GetExchange("/{id}")
    PostDto getById(@PathVariable("id") Long id);
}
```

- 순수한 **스펙(Specification)** — RestClient/WebClient 코드가 전혀 없음
- 엔진을 교체해도 이 파일은 수정하지 않음

### 2. `@ImportHttpServices` — Zero-Config 등록

```java
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
public class HttpClientConfig { ... }
```

| 속성 | 역할 |
|------|------|
| `group` | 논리적 서비스 그룹 이름. 같은 그룹은 동일한 RestClient 인스턴스를 공유 |
| `types` | 이 그룹에 포함할 `@HttpExchange` 인터페이스. Spring이 프록시를 생성하여 Bean 등록 |
| `clientType` | 엔진 선택: `REST_CLIENT` (기본) 또는 `WEB_CLIENT` |

기존 Spring 6의 `HttpServiceProxyFactory` + `RestClient.builder()` 보일러플레이트가 이 어노테이션 하나로 대체됨.

### 3. `RestClientHttpServiceGroupConfigurer` — Adapter (엔진 설정)

```java
@Bean
RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer() {
    return groups -> {
        groups.filterByName("jsonplaceholder")
                .forEachClient((group, clientBuilder) ->
                        clientBuilder
                                .baseUrl("https://jsonplaceholder.typicode.com")
                                .defaultHeader("Accept", "application/json"));

        groups.filterByName("dummyjson")
                .forEachClient((group, clientBuilder) ->
                        clientBuilder
                                .baseUrl("https://dummyjson.com")
                                .defaultHeader("Accept", "application/json"));
    };
}
```

- 그룹별로 **서로 다른 base URL**, 헤더, 인터셉터 등을 설정
- `application.yml`의 `spring.http.serviceclient.<group>.base-url`로도 설정 가능

### 4. 엔진 전환 방법 (RestClient → WebClient)

**변경이 필요한 곳: `HttpClientConfig.java` 단 1개 파일**

| 변경 항목 | Before | After |
|-----------|--------|-------|
| `clientType` | `REST_CLIENT` | `WEB_CLIENT` |
| Configurer 타입 | `RestClientHttpServiceGroupConfigurer` | `WebClientHttpServiceGroupConfigurer` |
| Builder 타입 | `RestClient.Builder` | `WebClient.Builder` |

**변경하지 않는 곳:**
- `@HttpExchange` 인터페이스 — 그대로
- `DemoController` — 그대로
- DTO — 그대로

이것이 **Adapter Pattern**의 핵심: 엔진 교체가 Config 한 곳에서만 발생.

## 실행 방법

```bash
./gradlew bootRun
```

## API 테스트

```bash
# JSONPlaceholder — 단일 포스트 조회
curl http://localhost:8080/posts/1

# JSONPlaceholder — 전체 포스트 조회
curl http://localhost:8080/posts

# DummyJSON — 단일 상품 조회
curl http://localhost:8080/products/1
```

### 응답 예시

**POST /posts/1**
```json
{
  "id": 1,
  "userId": 1,
  "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
  "body": "quia et suscipit..."
}
```

**GET /products/1**
```json
{
  "id": 1,
  "title": "Essence Mascara Lash Princess",
  "description": "The Essence Mascara Lash Princess is a popular mascara...",
  "price": 9.99,
  "brand": "Essence",
  "category": "beauty"
}
```

## Spring 6 vs Spring 7 비교

| 항목 | Spring 6 (기존) | Spring 7 (신규) |
|------|-----------------|-----------------|
| 클라이언트 등록 | `HttpServiceProxyFactory` 수동 설정 | `@ImportHttpServices` 자동 등록 |
| Base URL 설정 | `RestClient.builder().baseUrl(...)` 직접 지정 | Configurer 또는 `application.yml` |
| 그룹 관리 | 불가 (개별 빈 설정) | `group` 속성으로 논리적 그룹화 |
| 멀티 서비스 | 서비스마다 Bean 메서드 작성 | `@ImportHttpServices` 반복 선언 |
| 엔진 전환 | 전면 코드 수정 필요 | Config 1개 파일만 수정 |

## 참고 자료

- [HTTP Service Client Enhancements - Spring Blog](https://spring.io/blog/2025/09/23/http-service-client-enhancements/)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)
- [ImportHttpServices Javadoc](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/web/service/registry/ImportHttpServices.html)
- [RestClientHttpServiceGroupConfigurer Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/support/RestClientHttpServiceGroupConfigurer.html)
