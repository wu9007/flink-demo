package org.sword;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * @author chuan
 * @version 1.0
 * @since 2023/11/24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentationTest implements SmartInitializingSingleton {

    private final ObjectMapper objectMapper;
    private static final Marker INSTRUMENTATION_MARKER = MarkerFactory.getMarker("Instrumentation");

    @SneakyThrows
    @Override
    public void afterSingletonsInstantiated() {
        while (true) {
            Thread.sleep(5000);
            // 程序运行日志
            log.info("This is a program log message.");
            // 埋点日志
            log.info(INSTRUMENTATION_MARKER, "This is an instrumentation log message.");

            String uid = RandomUtil.randomNumbers(4);
            String event = RandomUtil.randomEle(ListUtil.toList("$register", "$login"));
            Instrumentation instrumentation = Instrumentation.builder()
                    .type("action")
                    .event(event)
                    .uid(uid)
                    .time(System.currentTimeMillis())
                    .properties(
                            MapBuilder.<String, Object>create()
                                    .put("deviceName", RandomUtil.randomEle(ListUtil.toList("oppo", "iphone", "xiaomi", "huawei")))
                                    .put("ip", "192.168.2." +  RandomUtil.randomInt(255))
                                    .build()
                    )
                    .build();
            log.info(INSTRUMENTATION_MARKER, objectMapper.writeValueAsString(instrumentation));
        }
    }

    @Data
    @Builder
    private static class Instrumentation implements Serializable {
        private String type;
        private String event;
        private String uid;
        private Long time;
        Map<String, Object> properties;
    }
}
