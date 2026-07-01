package com.github.zeng.alt.log.core.operation;

import com.github.zeng.alt.log.BusinessType;
import com.github.zeng.alt.log.OperatorType;
import lombok.Data;

@Data
public class LogOperation {
    private String title;
    private BusinessType businessType;
    private OperatorType operatorType;
    private boolean saveRequest;
    private boolean saveResponse;
    private String[] excludeParams;
}