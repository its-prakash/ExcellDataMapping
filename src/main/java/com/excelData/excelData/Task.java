package com.excelData.excelData;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table(name = "task")
@NoArgsConstructor
@Setter
@Getter
public class Task {

    @Id
    private Long id;
    private String projectName;
    private String taskName;
    private String assignedTo;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double progress;
    private String miscellaneous;

}
