package com.github.zeng.alt.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.exception.UtilException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年07月05日 20:40
 */
public record JacksonHelper(ObjectMapper objectMapper) {

	
	public String toJsonString(Object object) {
		if (ObjectUtils.isEmpty(object)) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new UtilException(e);
		}
	}

	
	public Map<String, Object> toMap(Object object) throws UtilException {
		if (ObjectUtils.isEmpty(object)) {
			return new HashMap<>();
		}
		return objectMapper.convertValue(object, new TypeReference<>() {});
	}

	
	public <T> T parseObject(String text, Class<T> clazz) {
		if (!StringUtils.hasText(text)) {
			return null;
		}
		try {
			return objectMapper.readValue(text, clazz);
		}
		catch (Exception e) {
			throw new UtilException(e);
		}
	}

	
	public <T> T parseObject(byte[] bytes, Class<T> clazz) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		try {
			return objectMapper.readValue(bytes, clazz);
		}
		catch (Exception e) {
			throw new UtilException(e);
		}
	}

	
	public <T> List<T> parseArray(String text, Class<T> clazz) {
		if (!StringUtils.hasText(text)) {
			return new ArrayList<>();
		}
		try {
			return objectMapper.readValue(text,
					objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
		}
		catch (IOException e) {
			throw new UtilException(e);
		}
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
