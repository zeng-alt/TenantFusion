package com.github.zeng.alt.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.exception.UtilException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
public record JacksonHelper(ObjectProvider<ObjectMapper> provider) {

	
	public String toJsonString(Object object) {
		if (ObjectUtils.isEmpty(object)) {
			return null;
		}
		try {
			return provider.getObject().writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new UtilException(e);
		}
	}

	
	public Map<String, Object> toMap(Object object) throws UtilException {
		if (ObjectUtils.isEmpty(object)) {
			return new HashMap<>();
		}
		return provider.getObject().convertValue(object, new TypeReference<>() {});
	}

	
	public <T> T parseObject(String text, Class<T> clazz) {
		if (!StringUtils.hasText(text)) {
			return null;
		}
		try {
			return provider.getObject().readValue(text, clazz);
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
			return provider.getObject().readValue(bytes, clazz);
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
			return provider.getObject().readValue(text,
					provider.getObject().getTypeFactory().constructCollectionType(List.class, clazz));
		}
		catch (IOException e) {
			throw new UtilException(e);
		}
	}

	public ObjectMapper getObjectMapper() {
		return provider.getObject();
	}

}
