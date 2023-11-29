package org.example.datastream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 从kafka读取数据后写入mysql
 * @author chuan
 * @version 1.0
 * @since 2023/11/26
 */
public class FlinkKafkaConsumerDemo {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnvironment.enableCheckpointing(TimeUnit.SECONDS.toMillis(1), CheckpointingMode.EXACTLY_ONCE);
        executionEnvironment.setParallelism(1);

        //从Kafka读取数据
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
        //写入Mysql
        dataStreamSource.addSink(
                JdbcSink.sink(
                        "insert into t_action (uid, action, time, ip, device_name) values (?,?,?,?,?)",
                        (statement, action)->{
                            statement.setString(1, action.getUid());
                            statement.setString(2, action.getEvent());
                            statement.setTimestamp(3, new Timestamp(action.getTime()));
                            Map<String, Object> properties = action.getProperties();
                            statement.setString(4, (String) properties.get("ip"));
                            statement.setString(5, (String) properties.get("deviceName"));
                        },
                        JdbcExecutionOptions.builder()
                                .withBatchSize(100)
                                .withBatchIntervalMs(20)
                                .withMaxRetries(2)
                                .build(),
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                                .withUrl("jdbc:mysql://localhost:3306/flinkdb?useTimezone=true&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true")
                                .withDriverName("com.mysql.cj.jdbc.Driver")
                                .withUsername("root")
                                .withPassword("root")
                                .build()
                        )
        );
        //打印到控制台
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
