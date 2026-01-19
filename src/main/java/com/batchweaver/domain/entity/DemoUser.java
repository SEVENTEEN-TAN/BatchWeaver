package com.batchweaver.domain.entity;

import com.batchweaver.domain.annotation.FileColumn;
import com.batchweaver.domain.converter.StringToDateConverter;
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

    // Getter/Setter methods (fallback if Lombok not working)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }
}
