package com.excelData.xml;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface XmlRepository extends ReactiveCrudRepository<Xml, Long> {
}
