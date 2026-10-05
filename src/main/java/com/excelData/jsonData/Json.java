package com.excelData.jsonData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("json_data")
public class Json {

    @Id
    private Long id;

    @Column("data")
    private String data; // Works directly as String now

    @Column("created_at")
    private LocalDateTime createdAt;
}