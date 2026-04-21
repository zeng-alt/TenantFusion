/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.zeng.alt.api.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Represents an HTTP request or response entity, consisting of headers and body.
 *
 * <p>Often used in combination with the {@link org.springframework.web.client.RestTemplate},
 * like so:
 * <pre class="code">
 * HttpHeaders headers = new HttpHeaders();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;&gt;("Hello World", headers);
 * URI location = template.postForLocation("https://example.com", entity);
 * </pre>
 * or
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 * Can also be used in Spring MVC, as a return value from a @Controller method:
 * <pre class="code">
 * &#64;GetMapping("/handle")
 * public HttpEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity&lt;&gt;("Hello World", responseHeaders);
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0.2
 * @param <T> the body type
 * @see org.springframework.web.client.RestTemplate
 * @see #getBody()
 */
public class HttpEntity<T> {

	/**
	 * An {@code HttpEntity} instance with a {@code null} body and
	 * {@link HttpHeaders#EMPTY empty headers}.
	 */
	public static final HttpEntity<?> EMPTY = new HttpEntity<>(HttpHeaders.EMPTY);

	@Nullable
	private final T body;


	/**
	 * Create a new, empty {@code HttpEntity}.
	 */
	protected HttpEntity() {
		this(null);
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and no headers.
	 * @param body the entity body
	 */
	public HttpEntity(T body) {
		this.body = body;
	}




	/**
	 * Returns the body of this entity.
	 */
	@Nullable
	public T getBody() {
		return this.body;
	}

	/**
	 * Indicates whether this entity has a body.
	 */
	public boolean hasBody() {
		return (this.body != null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		HttpEntity<?> otherEntity = (HttpEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.body, otherEntity.body);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(this.body);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		if (this.body != null) {
			builder.append(this.body);
			builder.append(',');
		}
		return builder.toString();
	}

}
