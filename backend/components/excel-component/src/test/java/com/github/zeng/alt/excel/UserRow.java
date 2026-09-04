package com.github.zeng.alt.excel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;

/**
 * 测试用实体：表头声明成 i18n key，用来同时覆盖字面量匹配与国际化匹配两条路径。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRow {

    @NotBlank(message = "姓名不能为空")
    @ExcelProperty("{excel.test.userName}")
    private String userName;

    @Min(value = 1, message = "年龄必须大于 0")
    @ExcelProperty("{excel.test.age}")
    private Integer age;
}
