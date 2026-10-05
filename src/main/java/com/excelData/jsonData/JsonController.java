package com.excelData.jsonData;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/json")
public class JsonController {

    private final JsonService jsonService;

    public JsonController(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @PostMapping
    public Mono<ResponseEntity<Json>> insertJson(@RequestBody String json) {
        return jsonService.createJson(json).map(ResponseEntity::ok);
    }

    @PostMapping("/bulk")
    public Flux<Json> insertJsonBulk(@RequestBody List<Map<String, Object>> jsonList) {
        return jsonService.createJsonBulk(jsonList);
    }
}
