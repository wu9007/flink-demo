package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author chuan
 * @version 1.0
 * @since 2023/11/26
 */
public class FlinkKafkaConsumerDemo {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnvironment.enableCheckpointing(TimeUnit.SECONDS.toMillis(1), CheckpointingMode.EXACTLY_ONCE);
        executionEnvironment.setParallelism(1);

        KafkaSource<Instrumentation> source = KafkaSource.<Instrumentation>builder()
                .setBootstrapServers("192.168.2.102:9092")
                .setTopics("logback-flume-kafka")
                .setGroupId("my-group")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(
                        new AbstractDeserializationSchema<Instrumentation>() {
                            @SneakyThrows
                            @Override
                            public Instrumentation deserialize(byte[] message) {
                                String logStr = new String(message, StandardCharsets.UTF_8);
                                String json = logStr.substring(logStr.indexOf("Instrumentation:") + 16);
                                ObjectMapper objectMapper = new ObjectMapper();
                                return objectMapper.readValue(json, Instrumentation.class);
                            }
                        })
                .build();

        DataStreamSource<Instrumentation> dataStreamSource = executionEnvironment.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
        dataStreamSource.print();
        executionEnvironment.execute();
    }


    @Data
    private static class Instrumentation implements Serializable {
        private String type;
        private String event;
        private String uid;
        private Long time;
        Map<String, Object> properties;
    }
}
