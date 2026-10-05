package com.excelData.jsonData;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JsonRepository extends ReactiveCrudRepository<Json, Long> {
}
