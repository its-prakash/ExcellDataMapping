package com.excelData.jsonData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class JsonServiceImpl implements JsonService {

    private final JsonRepository jsonRepository;
    private final ObjectMapper objectMapper;

    public JsonServiceImpl(JsonRepository jsonRepository, ObjectMapper objectMapper) {
        this.jsonRepository = jsonRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Json> createJson(String json) {
        Json jsonData = new Json();
        jsonData.setData(json);
        jsonData.setCreatedAt(LocalDateTime.now());
        return jsonRepository.save(jsonData);
    }

    @Override
    public Flux<Json> createJsonBulk(List<Map<String, Object>> jsonList) {
        return Flux.fromIterable(jsonList)
                .map(item -> {
                    Json jsonData = new Json();
                    try {
                        jsonData.setData(objectMapper.writeValueAsString(item));
                    } catch (JsonProcessingException e) {
                        jsonData.setData(item.toString());
                    }
                    jsonData.setCreatedAt(LocalDateTime.now());
                    return jsonData;
                })
                .buffer(500)
                .flatMap(jsonRepository::saveAll);
    }
}