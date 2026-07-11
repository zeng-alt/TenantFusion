package com.github.zeng.alt.json;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.github.zeng.alt.json.serialize.BigNumberConverter;
import com.github.zeng.alt.json.spi.DictServiceHolder;
import com.github.zeng.alt.json.spi.EncryptServiceHolder;
import com.github.zeng.alt.json.spi.IDictTranslateService;
import com.github.zeng.alt.json.spi.IEncryptService;
import io.vavr.jackson.datatype.VavrModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Role;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author zengJiaJun
 * @since 2026年07月11日
 * @version 1.0
 */
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class JsonConfiguration {

	@Bean
	@ConditionalOnMissingBean({VavrModule.class})
	public Module vavrModule() {
		return new VavrModule();
	}

	@Bean
	@ConditionalOnMissingBean({JavaTimeModule.class})
	public Module javaTimeModule() {
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(Long.class, BigNumberConverter.INSTANCE);
		javaTimeModule.addSerializer(Long.TYPE, BigNumberConverter.INSTANCE);
		javaTimeModule.addSerializer(BigInteger.class, BigNumberConverter.INSTANCE);
		javaTimeModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
		javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

		javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
		javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

		javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
		javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
		return javaTimeModule;
	}

	@Bean
	@ConditionalOnMissingBean
	@DependsOn({ "vavrModule", "javaTimeModule" })
	public JacksonHelper jsonHelper(ObjectProvider<ObjectMapper> provider) {
		return new JacksonHelper(provider);
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public static DictServiceHolder dictServiceHolder(ObjectProvider<IDictTranslateService> provider) {
		provider.ifAvailable(DictServiceHolder::setService);
		return new DictServiceHolder();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public static EncryptServiceHolder encryptServiceHolder(ObjectProvider<IEncryptService> provider) {
		provider.ifAvailable(EncryptServiceHolder::setService);
		return new EncryptServiceHolder();
	}

}
