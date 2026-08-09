package com.github.zeng.alt.workflow.service;

import com.github.zeng.alt.workflow.model.BusinessCreateCmd;
import com.github.zeng.alt.workflow.model.BusinessUpdateCmd;
import com.github.zeng.alt.workflow.model.BusinessVO;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.model.FormConfigVO;

import java.util.List;

/**
 * 业务服务接口
 *
 * @author zengAlt
 */
public interface BusinessService {

    List<BusinessVO> list();

    List<BusinessVO> tree();

    BusinessVO getDetail(Long id);

    BusinessVO create(BusinessCreateCmd cmd);

    BusinessVO update(Long id, BusinessUpdateCmd cmd);

    void delete(Long id);

    /**
     * 创建配置表单并关联到指定业务
     *
     * @param id  业务ID
     * @param cmd 配置表单创建参数
     * @return 关联后的业务
     */
    BusinessVO createAndBindFormConfig(Long id, FormConfigCreateCmd cmd);
}
