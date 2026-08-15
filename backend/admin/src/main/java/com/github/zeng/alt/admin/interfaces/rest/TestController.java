package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.domain.base.BasePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zengJiaJun
 * @since 2026年08月11日
 * @version 1.0
 */
@CrossOrigin
@Tag(name = "test接口")
@RestController
@RequestMapping("/v1/test")
public class TestController {

	private final Map<Long, TestData> map = new ConcurrentHashMap<>();
	private final AtomicLong idGenerator = new AtomicLong(1);

	private LocalDateTime createTime(int i) {
		return LocalDateTime.now().minusDays(i);
	}

	{
		LocalDate base = LocalDate.now();
		for (int i = 1; i <= 15; i++) {
			TestData d = new TestData();
			d.setId(idGenerator.getAndIncrement());
			d.setName("测试用户" + i);
			d.setAge(20 + i);
			d.setRank(100L * i);
			d.setEnabled(i % 2 == 0);
			d.setBirthday(base.minusDays(i * 30L));
			d.setCreateTime(createTime(i));
			map.put(d.getId(), d);
		}
	}

	@Operation(summary = "新增")
	@PostMapping
	public RestResponse<TestData> create(@RequestBody TestData data) {
		data.setId(idGenerator.getAndIncrement());
		data.setCreateTime(LocalDateTime.now());
		map.put(data.getId(), data);
		return RestResponse.success(data).message("新增成功");
	}

	@Operation(summary = "修改")
	@PutMapping("/{id}")
	public RestResponse<TestData> update(@PathVariable Long id, @RequestBody TestData data) {
		TestData old = map.get(id);
		if (old == null) {
			return RestResponse.fail("数据不存在");
		}
		data.setId(id);
		data.setCreateTime(old.getCreateTime());
		map.put(id, data);
		return RestResponse.success(data).message("修改成功");
	}

	@Operation(summary = "删除")
	@DeleteMapping("/{id}")
	public RestResponse<Object> delete(@PathVariable Long id) {
		map.remove(id);
		return RestResponse.success().message("删除成功");
	}

	@Operation(summary = "分页查询（支持多字段筛选）")
	@GetMapping
	public PageRestResponse<TestData> page(TestQuery query) {
		List<TestData> all = map.values().stream()
				.filter(d -> query.getName() == null || d.getName().contains(query.getName()))
				.filter(d -> query.getAge() == null || query.getAge().equals(d.getAge()))
				.filter(d -> query.getMinAge() == null || d.getAge() >= query.getMinAge())
				.filter(d -> query.getMaxAge() == null || d.getAge() <= query.getMaxAge())
				.filter(d -> query.getRank() == null || query.getRank().equals(d.getRank()))
				.filter(d -> query.getEnabled() == null || query.getEnabled().equals(d.getEnabled()))
				.filter(d -> query.getBirthday() == null || query.getBirthday().equals(d.getBirthday()))
				.sorted(Comparator.comparing(TestData::getId).reversed())
				.toList();
		int start = (query.getPageNo() - 1) * query.getPageSize();
		start = Math.min(start, all.size());
		int end = Math.min(start + query.getPageSize(), all.size());
		return PageRestResponse.of(all.subList(start, end), all.size(), query.getPageSize(), query.getPageNo());
	}

	/**
	 * 测试查询条件：分页 + 多字段筛选（包装类型，null 表示不筛选）
	 */
	@Data
	public static class TestQuery extends BasePage {
		private String name;
		private Integer age;
		private Integer minAge;
		private Integer maxAge;
		private Long rank;
		private Boolean enabled;
		private java.time.LocalDate birthday;
	}

	/**
	 * 测试结果数据结构：string、Integer、Long、Boolean、日期、日期时间（全部使用包装类型）
	 */
	@Data
	public static class TestData {
		private Long id;
		private String name;
		private Integer age;
		private Long rank;
		private Boolean enabled;
		private LocalDate birthday;
		private LocalDateTime createTime;
	}
}