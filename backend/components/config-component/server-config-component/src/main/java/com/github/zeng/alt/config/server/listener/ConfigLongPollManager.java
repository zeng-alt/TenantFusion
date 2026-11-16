package com.github.zeng.alt.config.server.listener;

import com.github.zeng.alt.config.event.ConfigDataChangedEvent;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.async.DeferredResult;

public class ConfigLongPollManager {

    private final Map<String, List<DeferredResult<List<String>>>> pollRequests = new ConcurrentHashMap<>();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onConfigDataChanged(ConfigDataChangedEvent event) {
        notifyClients(event.getAppCode(), event.getItems().stream().map(ConfigItemDTO::getDataId).distinct().toList());
    }

    public void register(String appCode, DeferredResult<List<String>> deferredResult) {
        pollRequests.computeIfAbsent(appCode, k -> new CopyOnWriteArrayList<>())
                .add(deferredResult);

        deferredResult.onCompletion(() -> {
            List<DeferredResult<List<String>>> list = pollRequests.get(appCode);
            if (list != null) {
                list.remove(deferredResult);
            }
        });

        deferredResult.onTimeout(() -> {
            List<DeferredResult<List<String>>> list = pollRequests.get(appCode);
            if (list != null) {
                list.remove(deferredResult);
            }
        });
    }

    public void notifyClients(String appCode, List<String> changedKeys) {
        String key = appCode;
        if (key == null) {
            for (Map.Entry<String, List<DeferredResult<List<String>>>> entry : pollRequests.entrySet()) {
                for (DeferredResult<List<String>> result : entry.getValue()) {
                    if (result.isSetOrExpired()) continue;
                    result.setResult(changedKeys);
                }
                entry.getValue().clear();
            }
            return;
        }

        List<DeferredResult<List<String>>> list = pollRequests.get(key);
        if (list == null || list.isEmpty()) return;

        for (DeferredResult<List<String>> result : list) {
            if (result.isSetOrExpired()) continue;
            result.setResult(changedKeys);
        }
        list.clear();
    }
}
