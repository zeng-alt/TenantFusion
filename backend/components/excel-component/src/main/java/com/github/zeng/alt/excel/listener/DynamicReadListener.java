package com.github.zeng.alt.excel.listener;

import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.apache.fesod.sheet.metadata.FieldCache;
import org.apache.fesod.sheet.metadata.FieldWrapper;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.springframework.core.GenericTypeResolver;
import com.github.zeng.alt.excel.dynamic.AbaDynamicColumn;
import com.github.zeng.alt.excel.dynamic.DynamicEntity;
import com.github.zeng.alt.excel.dynamic.InterfaceDynamicColumn;
import com.github.zeng.alt.excel.exception.DynamicReadExcelException;
import com.github.zeng.alt.excel.utils.ValidaHelper;
import com.github.zeng.alt.i18n.MessageSourceHelper;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import lombok.Getter;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月01日 21:33
 */
@Getter
public abstract class DynamicReadListener<T extends InterfaceDynamicColumn<E>, E extends DynamicEntity> extends AnalysisEventListener<Map<Integer, ReadCellData<?>>> {

    protected Map<Integer, Tuple2<String, String>> headMap = new LinkedHashMap<>();
    protected final Class<T> clazz;
    protected Class<E> dynamicClazz;


    public DynamicReadListener(Class<T> clazz) {
        Assert.notNull(clazz, "请指定泛型");
        this.clazz = clazz;
        // 原先调用 com.zjj.core.component.utils.ClassUtils.findGenericType，
        // 那个工具类在包重命名时丢失、全仓已无实现。改用 Spring 自带的
        // GenericTypeResolver：它会沿类与接口的整条继承链解析实参，
        // 因此只需给出 InterfaceDynamicColumn——AbaDynamicColumn 本就实现了它。
        Class<?>[] typeArguments =
                GenericTypeResolver.resolveTypeArguments(clazz, InterfaceDynamicColumn.class);
        if (typeArguments != null && typeArguments.length > 0) {
            this.dynamicClazz = (Class<E>) typeArguments[0];
        }
        Assert.notNull(dynamicClazz, "请指定动态列泛型");
    }

    public DynamicReadListener(Class<T> clazz, Class<E> dynamicClazz) {
        Assert.notNull(clazz, "请指定泛型");
        Assert.notNull(dynamicClazz, "请指定动态列泛型");
        this.clazz = clazz;
        this.dynamicClazz = dynamicClazz;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            String value = entry.getValue();
            String template = value;
            if (value.startsWith("{") && value.endsWith("}")) {
                template = MessageSourceHelper.getMessage(value, value);
            }
            this.headMap.put(entry.getKey(), Tuple.of(value, template));
        }
    }

    @Override
    public void invoke(Map<Integer, ReadCellData<?>> cellData, AnalysisContext analysisContext) {
        T object = instantiateObject();

        FieldCache fieldCache = org.apache.fesod.sheet.util.ClassUtils.declaredFields(clazz, analysisContext.currentReadHolder());
        Map<Integer, String> dataMap = cellData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()), (v1, v2) -> v1));
        Set<Integer> dataIndex = dataMap.keySet();

        Map<Integer, FieldWrapper> sortedFieldMap = fieldCache.getSortedFieldMap();

        for (Map.Entry<Integer, FieldWrapper> entry : sortedFieldMap.entrySet()) {
            FieldWrapper value = entry.getValue();
            String[] heads = value.getHeads();
            if (heads == null || heads.length == 0) {
                continue;
            }

            String data = dataMap.get(entry.getKey());


            Option
                    .of(BeanUtils.getPropertyDescriptor(object.getClass(), value.getFieldName()))
                    .map(PropertyDescriptor::getWriteMethod)
                    .forEach(m -> {
                        try {
                            m.invoke(object, data);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new DynamicReadExcelException("访问 [" + value.getFieldName() + "] 的set方法权限不够，或者没有找到set方法！！！");
                        }
                    });

            dataIndex.remove(entry.getKey());

        }


        for (Integer index : dataIndex) {
            E dynamicEntity = BeanUtils.instantiateClass(dynamicClazz);
            String data = dataMap.get(index);
            dynamicEntity.setIndex(index);
            Tuple2<String, String> head = headMap.get(index);
            if (head != null) {
                dynamicEntity.setName(head._1);
                dynamicEntity.setNameTemplate(head._2);
            }
            dynamicEntity.setValue(data);
            object.add(dynamicEntity);
        }

        ValidaHelper.validate(object, analysisContext);
        this.invokeObject(object, analysisContext);
    }

    public T instantiateObject() {
        return BeanUtils.instantiateClass(clazz);
    }

    public abstract void invokeObject(T t, AnalysisContext context);
}
