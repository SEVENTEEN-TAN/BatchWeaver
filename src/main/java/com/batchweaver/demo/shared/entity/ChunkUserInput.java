package com.batchweaver.demo.shared.entity;

import com.batchweaver.core.annotation.FileColumn;
import lombok.Data;

import java.util.Date;

/**
 * Demo 用户文件输入 DTO
 * <p>
 * 用于解析包含 age 字段的文件数据，age 字段不写入数据库
 */
@Data
public class ChunkUserInput {


    /**
     * 用户姓名
     * 文件列索引: 0
     */
    @FileColumn(index = 0, name = "userName", trim = true)
    private String name;

    /**
     * 用户年龄
     * 文件列索引: 1
     * 注意：此字段仅用于文件解析，不写入数据库
     */
    @FileColumn(index = 1, name = "age")
    private Integer age;

    /**
     * 用户邮箱
     * 文件列索引: 2
     */
    @FileColumn(index = 2, name = "email", trim = true)
    private String email;

    /**
     * 出生日期
     * 文件列索引: 3
     */
    @FileColumn(index = 3, name = "birthDate", format = "yyyy-MM-dd")
    private Date birthDate;
}
