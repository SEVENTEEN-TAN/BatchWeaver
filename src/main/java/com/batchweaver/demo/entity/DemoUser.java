package com.batchweaver.demo.entity;

import com.batchweaver.core.annotation.FileColumn;
import com.batchweaver.core.converter.StringToDateConverter;
import lombok.Data;

import java.util.Date;

/**
 * 示例实体类 - DemoUser
 * <p>
 * 展示 @FileColumn 注解的使用方式
 */
@Data
public class DemoUser {

    @FileColumn(index = 0, name = "userId")
    private Integer id;

    @FileColumn(index = 1, name = "userName", trim = true, toUpperCase = true)
    private String name;

    @FileColumn(index = 2, name = "email", trim = true, defaultValue = "unknown@example.com")
    private String email;

    @FileColumn(index = 3, name = "birthDate", converter = StringToDateConverter.class)
    private Date birthDate;

}
