package org.example.datastream;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 读取mysql数据
 * @author chuan
 * @version 1.0
 * @since 2023/11/26
 */
public class FlinkMysqlConsumerDemo {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(1);
        // enable checkpoint
        environment.enableCheckpointing(5000);
        environment.getCheckpointConfig().setCheckpointTimeout(10000);
        environment.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        environment.getCheckpointConfig().setMaxConcurrentCheckpoints(1);

        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("192.168.2.104")
                .port(3306)
                .databaseList("flinkdb")
                .tableList("flinkdb.t_action")
                .username("root")
                .password("root")
                .deserializer(new JsonDebeziumDeserializationSchema())
                .includeSchemaChanges(true)
                .startupOptions(StartupOptions.latest())
                .build();

        DataStreamSource<String> dataStreamSource = environment.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
        dataStreamSource.print();
        environment.execute("Print MySQL Snapshot + Binlog");
    }
}
