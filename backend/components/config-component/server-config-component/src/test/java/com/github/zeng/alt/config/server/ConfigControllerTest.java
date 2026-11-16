package com.github.zeng.alt.config.server;

import com.github.zeng.alt.config.server.controller.ConfigLongPollController;
import com.github.zeng.alt.config.server.controller.ConfigManageController;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import com.github.zeng.alt.config.server.listener.ConfigLongPollManager;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import io.vavr.control.Either;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.async.DeferredResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST 命令绑定和长轮询隔离测试。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ConfigControllerTest {
    @Test
    void acceptsLongIdsAndRejectsMissingRequiredContent() throws Exception {
        final var manage = mock(ConfigManageService.class);
        final var publish = mock(ConfigPublishService.class);
        when(publish.publish(any(ConfigPublishCmd.class))).thenReturn(Either.right(ConfigReleaseDto.builder().build()));
        final var mvc = MockMvcBuilders.standaloneSetup(new ConfigManageController(manage, publish)).build();

        mvc.perform(post("/api/config/admin/publish").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appCode\":\"shop\",\"configIds\":[4000000000]}"))
                .andExpect(status().isOk());
        final var command = ArgumentCaptor.forClass(ConfigPublishCmd.class);
        verify(publish).publish(command.capture());
        assertThat(command.getValue().configIds()).containsExactly(4000000000L);
        mvc.perform(post("/api/config/admin/config").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appCode\":\"shop\",\"dataId\":\"enabled\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notifiesOnlyTheMatchingApplication() {
        final var manager = new ConfigLongPollManager();
        final var controller = new ConfigLongPollController(manager);
        final DeferredResult<List<String>> shop = controller.listen("shop", 30000);
        final DeferredResult<List<String>> other = controller.listen("other", 30000);

        manager.notifyClients("shop", List.of("enabled"));

        assertThat(shop.getResult()).isEqualTo(List.of("enabled"));
        assertThat(other.hasResult()).isFalse();
        manager.notifyClients("shop", List.of("next"));
        assertThat(shop.getResult()).isEqualTo(List.of("enabled"));
    }
}
