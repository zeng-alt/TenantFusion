package com.github.zeng.alt.excel.listener;

import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.metadata.FieldCache;
import org.apache.fesod.sheet.metadata.FieldWrapper;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.apache.fesod.sheet.util.ClassUtils;
import org.apache.fesod.sheet.util.ConverterUtils;
import com.github.zeng.alt.i18n.MessageSourceHelper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.stream.Collectors;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月04日 21:20
 */
public interface I18nReadListener<T, R> extends ReadListener<R> {

    @Override
    default void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        invokeHeadMap(this.parseHeadMap(headMap, context), context);
    }

    void invokeHeadMap(Map<Integer, List<Head>> headMap, AnalysisContext context);

    default Map<Integer, List<Head>> parseHeadMap(Map<Integer, ReadCellData<?>> heads, AnalysisContext context) {
        Map<Integer, List<Head>> result = new HashMap<>();
        Map<String, Integer> inverseHeadMap = MapUtils.invertMap(ConverterUtils.convertToStringMap(heads, context));
        // 拿到字段对应的set方法
        Map<String, PropertyDescriptor> propertyDescriptors = Arrays
                                            .stream(BeanUtils.getPropertyDescriptors(getEntityClass()))
                                            .skip(1L)
                                            .collect(Collectors.toMap(PropertyDescriptor::getName, p->p));
        FieldCache fieldCache = ClassUtils.declaredFields(getEntityClass(), context.readSheetHolder());

        // 拿到字段对应的@ExcelProperty中的value [value, [字段名]]
        Multimap<String, String> filedMap = fieldCache
                                                .getSortedFieldMap()
                                                .values()
                                                .stream()
                                                .collect(ArrayListMultimap::create, (m, i) -> m.put(parseHead(i), i.getFieldName()), Multimap::putAll);
        for (Map.Entry<String, Integer> entry : inverseHeadMap.entrySet()) {
            Collection<String> fields = filedMap.get(entry.getKey());
            List<Head> list = new ArrayList<>();
//            ReadCellData<?> readCellData = heads.get(entry.getValue());
            
            for (String field : fields) {
//                ExcelContentProperty excelContentProperty = ClassUtils.declaredExcelContentProperty(dataMap, context.readSheetHolder().excelReadHeadProperty().getHeadClazz(),
//                        field, context.readSheetHolder());
                PropertyDescriptor propertyDescriptor = propertyDescriptors.get(field);
                if (propertyDescriptor == null) continue;
                list.add(new Head(propertyDescriptor.getWriteMethod(), propertyDescriptor.getPropertyType(), field));
            }
            result.put(entry.getValue(), list);
        }
        return result;
    }

    static String parseHead(FieldWrapper fieldWrapper) {
        String[] heads = fieldWrapper.getHeads();
        if (ArrayUtils.isEmpty(heads)) {
            return fieldWrapper.getFieldName();
        }
        return MessageSourceHelper.toMessage(heads[0]);
    }


    Class<T> getEntityClass();
}
