package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.model.FormConfigOptionVO;
import com.github.zeng.alt.workflow.model.FormConfigQuery;
import com.github.zeng.alt.workflow.model.FormConfigSaveDraftCmd;
import com.github.zeng.alt.workflow.model.FormConfigUpdateCmd;
import com.github.zeng.alt.workflow.model.FormConfigVO;
import com.github.zeng.alt.workflow.model.FormConfigVersionVO;

import java.util.List;

/**
 * 配置表单服务接口
 *
 * @author zengAlt
 */
public interface FormConfigService {

    PageRestResponse<FormConfigVO> page(FormConfigQuery query);

    List<FormConfigOptionVO> options();

    FormConfigVO getDetail(Long id);

    FormConfigVO create(FormConfigCreateCmd cmd);

    FormConfigVO update(Long id, FormConfigUpdateCmd cmd);

    void delete(Long id);

    List<FormConfigVersionVO> versions(Long formConfigId);

    FormConfigVersionVO getVersion(Long formConfigId, Integer version);

    FormConfigVersionVO getVersion(Long versionId);

    FormConfigVersionVO saveDraft(Long formConfigId, FormConfigSaveDraftCmd cmd);

    FormConfigVersionVO saveAndPublish(Long formConfigId, FormConfigSaveDraftCmd cmd);

    FormConfigVersionVO publish(Long versionId);

    FormConfigVersionVO offline(Long versionId);
}
