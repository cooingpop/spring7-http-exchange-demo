package com.example.httptest.adapter.out.http.client;

import com.example.httptest.adapter.out.http.dto.ProductDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * DummyJSON 외부 API를 선언적으로 정의하는 HTTP 인터페이스.
 *
 * <p>JSONPlaceholder와는 다른 서비스(다른 base URL)이며,
 * Spring 7의 "그룹(group)" 기능을 활용해 서비스별로 독립 설정이 가능하다.
 *
 * <h3>멀티 서비스 구조</h3>
 * <pre>
 * jsonplaceholder 그룹 → https://jsonplaceholder.typicode.com
 * dummyjson 그룹       → https://dummyjson.com
 * </pre>
 * 각 그룹은 서로 다른 base URL, timeout, 헤더 등을 가질 수 있다.
 */
@HttpExchange("/products")
public interface DummyJsonClient {

    @GetExchange("/{id}")
    ProductDto getById(@PathVariable("id") Long id);
}
