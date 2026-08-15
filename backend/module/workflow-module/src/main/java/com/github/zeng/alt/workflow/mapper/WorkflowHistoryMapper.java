package com.github.zeng.alt.workflow.mapper;

import com.github.zeng.alt.camunda.engine.api.history.HistoricActivityInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricTaskInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricVariableInfo;
import com.github.zeng.alt.workflow.model.HistoricActivityVO;
import com.github.zeng.alt.workflow.model.HistoricProcessInstanceVO;
import com.github.zeng.alt.workflow.model.HistoricVariableVO;
import com.github.zeng.alt.workflow.model.TaskVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * 历史数据 MapStruct 映射：引擎 SPI 信息 → VO。
 *
 * @author zengAlt
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WorkflowHistoryMapper {

    @Mapping(target = "startUserName", source = "startUserId")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "status", ignore = true)
    HistoricProcessInstanceVO toProcessInstanceVO(HistoricProcessInstanceInfo info);

    @AfterMapping
    default void fillProcessInstanceState(HistoricProcessInstanceInfo info, @MappingTarget HistoricProcessInstanceVO vo) {
        if (info.getEndTime() != null) {
            if (info.getDeleteReason() != null) {
                vo.setState("deleted");
                vo.setStatus("terminated");
            } else {
                vo.setState("completed");
                vo.setStatus("completed");
            }
        } else {
            vo.setState("active");
            vo.setStatus("running");
        }
    }

    @Mapping(target = "createTime", source = "startTime")
    TaskVO toTaskVO(HistoricTaskInfo info);

    @AfterMapping
    default void fillTaskPriority(HistoricTaskInfo info, @MappingTarget TaskVO vo) {
        if (info.getPriority() == null || info.getPriority() <= 0) {
            vo.setPriority(null);
        }
    }

    HistoricActivityVO toActivityVO(HistoricActivityInfo info);

    HistoricVariableVO toVariableVO(HistoricVariableInfo info);
}
