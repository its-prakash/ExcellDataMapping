package com.excelData.xml;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;


@Getter
@Setter
@NoArgsConstructor
@Table("employees")
public class Xml {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("salary")
    private double salary;

    @Column("company")
    private String company;

    @Column("role")
    private String role;
}
