package org.example.datastream;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author chuan
 * @version 1.0
 * @since 2023/11/26
 */
public class FlinkKafkaProducerDemo {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnvironment.setParallelism(1);

        List<String> wordList = new ArrayList<>();
        wordList.add("Hello");
        wordList.add("World");
        DataStreamSource<String> dataStream = executionEnvironment.fromCollection(wordList);

        Properties producerProperties = new Properties();
        producerProperties.setProperty("transaction.timeout.ms", "60000");
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("192.168.2.102:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("flink-demo")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setKafkaProducerConfig(producerProperties)
                .setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        dataStream.sinkTo(sink);
        executionEnvironment.execute();
    }
}
