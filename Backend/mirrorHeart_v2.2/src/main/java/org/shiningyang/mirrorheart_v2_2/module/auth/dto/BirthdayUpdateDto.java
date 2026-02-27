package org.shiningyang.mirrorheart_v2_2.module.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BirthdayUpdateDto {
    @NotNull(message = "生日不能为空")
    private LocalDate birthday;
}