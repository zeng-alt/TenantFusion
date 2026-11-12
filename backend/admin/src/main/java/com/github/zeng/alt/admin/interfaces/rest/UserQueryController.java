package com.github.zeng.alt.admin.interfaces.rest;

import app.tozzi.exception.JPASearchException;
import app.tozzi.model.input.JPASearchInput;
import com.github.zeng.alt.admin.infrastructure.projection.UserDto;
import com.github.zeng.alt.admin.query.api.UserQueryService;
import com.github.zeng.alt.admin.query.api.dto.UserPageQuery;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户数据查询接口，保留既有列表和详情路径及返回包装。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserQueryController {
    private final UserQueryService queries;

    @GetMapping
    public PageRestResponse<UserDto> page(@Valid @ModelAttribute UserPageQuery query) {
        return response(queries.page(query).getOrElseThrow(AccessDeniedException::new));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        return queries.getDetail(id).getOrElseThrow(AccessDeniedException::new).fold(
                () -> ResponseEntity.status(404).body(RestResponse.fail("用户不存在")),
                user -> ResponseEntity.ok(RestResponse.success(user)));
    }

    @PostMapping("/search")
    public PageRestResponse<UserDto> search(@Valid @RequestBody JPASearchInput input) {
        return response(queries.search(input).getOrElseThrow(AccessDeniedException::new));
    }

    /** 用户查询参数错误在本控制器内渲染，保持其他业务的异常契约。 */
    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class,
            JPASearchException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> invalidQuery(Exception exception) {
        final var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "用户查询参数无效");
        return ResponseEntity.badRequest().body(problem);
    }

    private PageRestResponse<UserDto> response(Page<UserDto> page) {
        return PageRestResponse.of(page.getContent(), page.getTotalElements(), page.getSize(), page.getNumber() + 1);
    }
}
