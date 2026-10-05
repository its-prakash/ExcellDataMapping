package com.excelData.jsonData;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

public interface JsonService {

    Mono<Json> createJson(String json);

    Flux<Json> createJsonBulk(List<Map<String, Object>> jsonList);

}
