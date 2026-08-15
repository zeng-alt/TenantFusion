package com.github.zeng.alt.camunda.engine.remote.history;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.history.HistoricActivityInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricProcessInstanceQuery;
import com.github.zeng.alt.camunda.engine.api.history.HistoricTaskInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoricVariableInfo;
import com.github.zeng.alt.camunda.engine.api.history.HistoryApi;
import com.github.zeng.alt.camunda.engine.remote.RemoteSupport;
import org.camunda.community.rest.client.api.HistoryApiClient;
import org.camunda.community.rest.client.model.HistoricActivityInstanceDto;
import org.camunda.community.rest.client.model.HistoricActivityInstanceQueryDto;
import org.camunda.community.rest.client.model.HistoricActivityInstanceQueryDtoSortingInner;
import org.camunda.community.rest.client.model.HistoricProcessInstanceDto;
import org.camunda.community.rest.client.model.HistoricProcessInstanceQueryDto;
import org.camunda.community.rest.client.model.HistoricProcessInstanceQueryDtoSortingInner;
import org.camunda.community.rest.client.model.HistoricTaskInstanceDto;
import org.camunda.community.rest.client.model.HistoricTaskInstanceQueryDto;
import org.camunda.community.rest.client.model.HistoricTaskInstanceQueryDtoSortingInner;
import org.camunda.community.rest.client.model.HistoricVariableInstanceDto;
import org.camunda.community.rest.client.model.HistoricVariableInstanceQueryDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 远程历史数据实现
 * <p>
 * 发起人取 START_USER_ID_（远程模式下为引擎认证用户/服务账号，受 camunda:initiator 覆盖，与变量一致）。
 *
 * @author zengAlt
 */
@Service
public class RemoteHistoryApi implements HistoryApi {

    private final HistoryApiClient historyApiClient;

    public RemoteHistoryApi(HistoryApiClient historyApiClient) {
        this.historyApiClient = historyApiClient;
    }

    @Override
    public PageRestResponse<HistoricProcessInstanceInfo> queryProcessInstances(HistoricProcessInstanceQuery query) {
        HistoricProcessInstanceQueryDto dto = new HistoricProcessInstanceQueryDto();
        dto.setProcessDefinitionKey(query.getProcessDefinitionKey());
        if (StringUtils.hasText(query.getProcessDefinitionName())) {
            dto.setProcessDefinitionNameLike("%" + query.getProcessDefinitionName() + "%");
        }
        dto.setProcessInstanceBusinessKey(query.getBusinessKey());
        if (StringUtils.hasText(query.getState())) {
            switch (query.getState()) {
                case "running" -> dto.setUnfinished(true);
                case "completed", "terminated" -> dto.setFinished(true);
                case "suspended" -> dto.setSuspended(true);
                default -> {
                }
            }
        }
        if (StringUtils.hasText(query.getStartUserId())) {
            dto.setStartedBy(query.getStartUserId());
        }
        if (StringUtils.hasText(query.getInitiator())) {
            dto.setStartedBy(query.getInitiator());
        }
        HistoricProcessInstanceQueryDtoSortingInner sorting = new HistoricProcessInstanceQueryDtoSortingInner();
        sorting.setSortBy(HistoricProcessInstanceQueryDtoSortingInner.SortByEnum.START_TIME);
        sorting.setSortOrder(HistoricProcessInstanceQueryDtoSortingInner.SortOrderEnum.DESC);
        dto.setSorting(List.of(sorting));

        long total = historyApiClient.queryHistoricProcessInstancesCount(dto).getBody().getCount();
        int firstResult = (query.getPageNo() - 1) * query.getPageSize();
        List<HistoricProcessInstanceDto> list = historyApiClient
                .queryHistoricProcessInstances(firstResult, query.getPageSize(), dto).getBody();
        List<HistoricProcessInstanceInfo> vos = list.stream().map(this::toProcessInstanceInfo).toList();
        return PageRestResponse.of(vos, total, query.getPageSize(), query.getPageNo());
    }

    @Override
    public HistoricProcessInstanceInfo getProcessInstance(String processInstanceId) {
        HistoricProcessInstanceDto dto = historyApiClient.getHistoricProcessInstance(processInstanceId).getBody();
        if (dto == null) {
            throw new IllegalStateException("历史流程实例不存在: " + processInstanceId);
        }
        return toProcessInstanceInfo(dto);
    }

    @Override
    public PageRestResponse<HistoricTaskInfo> queryTasks(String assignee, String processInstanceId, Boolean finished,
                                                         int pageNo, int pageSize) {
        HistoricTaskInstanceQueryDto dto = new HistoricTaskInstanceQueryDto();
        dto.setTaskAssignee(assignee);
        dto.setProcessInstanceId(processInstanceId);
        if (finished != null && finished) {
            dto.setFinished(true);
        } else if (finished != null) {
            dto.setUnfinished(true);
        }
        HistoricTaskInstanceQueryDtoSortingInner sorting = new HistoricTaskInstanceQueryDtoSortingInner();
        sorting.setSortBy(HistoricTaskInstanceQueryDtoSortingInner.SortByEnum.END_TIME);
        sorting.setSortOrder(HistoricTaskInstanceQueryDtoSortingInner.SortOrderEnum.DESC);
        dto.setSorting(List.of(sorting));

        long total = historyApiClient.queryHistoricTaskInstancesCount(dto).getBody().getCount();
        int firstResult = (pageNo - 1) * pageSize;
        List<HistoricTaskInstanceDto> list = historyApiClient
                .queryHistoricTaskInstances(firstResult, pageSize, dto).getBody();
        List<HistoricTaskInfo> vos = list.stream().map(this::toTaskInfo).toList();
        return PageRestResponse.of(vos, total, pageSize, pageNo);
    }

    @Override
    public List<HistoricActivityInfo> activities(String processInstanceId) {
        HistoricActivityInstanceQueryDto dto = new HistoricActivityInstanceQueryDto();
        dto.setProcessInstanceId(processInstanceId);
        HistoricActivityInstanceQueryDtoSortingInner sorting = new HistoricActivityInstanceQueryDtoSortingInner();
        sorting.setSortBy(HistoricActivityInstanceQueryDtoSortingInner.SortByEnum.START_TIME);
        sorting.setSortOrder(HistoricActivityInstanceQueryDtoSortingInner.SortOrderEnum.ASC);
        dto.setSorting(List.of(sorting));
        return historyApiClient.queryHistoricActivityInstances(0, Integer.MAX_VALUE, dto).getBody().stream()
                .map(this::toActivityInfo).toList();
    }

    @Override
    public PageRestResponse<HistoricVariableInfo> queryVariables(String processInstanceId, String variableName,
                                                                 int pageNo, int pageSize) {
        HistoricVariableInstanceQueryDto dto = new HistoricVariableInstanceQueryDto();
        dto.setProcessInstanceId(processInstanceId);
        dto.setVariableName(variableName);
        long total = historyApiClient.queryHistoricVariableInstancesCount(dto).getBody().getCount();
        int firstResult = (pageNo - 1) * pageSize;
        List<HistoricVariableInstanceDto> list = historyApiClient
                .queryHistoricVariableInstances(firstResult, pageSize, false, dto).getBody();
        List<HistoricVariableInfo> vos = list.stream().map(this::toVariableInfo).toList();
        return PageRestResponse.of(vos, total, pageSize, pageNo);
    }

    @Override
    public List<HistoricVariableInfo> variables(String processInstanceId) {
        HistoricVariableInstanceQueryDto dto = new HistoricVariableInstanceQueryDto();
        dto.setProcessInstanceId(processInstanceId);
        return historyApiClient.queryHistoricVariableInstances(0, Integer.MAX_VALUE, false, dto).getBody().stream()
                .map(this::toVariableInfo).toList();
    }

    private HistoricProcessInstanceInfo toProcessInstanceInfo(HistoricProcessInstanceDto dto) {
        return HistoricProcessInstanceInfo.builder()
                .id(dto.getId())
                .businessKey(dto.getBusinessKey())
                .processDefinitionId(dto.getProcessDefinitionId())
                .processDefinitionKey(dto.getProcessDefinitionKey())
                .processDefinitionName(dto.getProcessDefinitionName())
                .processDefinitionVersion(dto.getProcessDefinitionVersion())
                .startTime(RemoteSupport.toLocalDateTime(dto.getStartTime()))
                .endTime(RemoteSupport.toLocalDateTime(dto.getEndTime()))
                .durationInMillis(dto.getDurationInMillis())
                .startUserId(dto.getStartUserId())
                .initiator(dto.getStartUserId())
                .deleteReason(dto.getDeleteReason())
                .tenantId(dto.getTenantId())
                .build();
    }

    private HistoricTaskInfo toTaskInfo(HistoricTaskInstanceDto dto) {
        return HistoricTaskInfo.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .taskDefinitionKey(dto.getTaskDefinitionKey())
                .assignee(dto.getAssignee())
                .owner(dto.getOwner())
                .processInstanceId(dto.getProcessInstanceId())
                .processDefinitionId(dto.getProcessDefinitionId())
                .startTime(RemoteSupport.toLocalDateTime(dto.getStartTime()))
                .endTime(RemoteSupport.toLocalDateTime(dto.getEndTime()))
                .dueDate(RemoteSupport.toLocalDateTime(dto.getDue()))
                .priority(dto.getPriority())
                .build();
    }

    private HistoricActivityInfo toActivityInfo(HistoricActivityInstanceDto dto) {
        return HistoricActivityInfo.builder()
                .id(dto.getId())
                .activityId(dto.getActivityId())
                .activityName(dto.getActivityName())
                .activityType(dto.getActivityType())
                .assignee(dto.getAssignee())
                .processInstanceId(dto.getProcessInstanceId())
                .executionId(dto.getExecutionId())
                .taskId(dto.getTaskId())
                .startTime(RemoteSupport.toLocalDateTime(dto.getStartTime()))
                .endTime(RemoteSupport.toLocalDateTime(dto.getEndTime()))
                .durationInMillis(dto.getDurationInMillis())
                .build();
    }

    private HistoricVariableInfo toVariableInfo(HistoricVariableInstanceDto dto) {
        return HistoricVariableInfo.builder()
                .id(dto.getId())
                .name(dto.getName())
                .value(dto.getValue())
                .type(dto.getType())
                .processInstanceId(dto.getProcessInstanceId())
                .executionId(dto.getExecutionId())
                .activityInstanceId(dto.getActivityInstanceId())
                .build();
    }
}
