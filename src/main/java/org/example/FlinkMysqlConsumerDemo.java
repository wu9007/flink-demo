package org.example;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
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

        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("127.0.0.1")
                .port(3306)
                .databaseList("flinkdb")
                .tableList("flinkdb.t_action_ods")
                .username("root")
                .password("root")
                .deserializer(new JsonDebeziumDeserializationSchema())
                .includeSchemaChanges(true)
                .startupOptions(StartupOptions.latest())
                .build();

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(1);
        // enable checkpoint
        environment.enableCheckpointing(3000);

        DataStreamSource<String> dataStreamSource = environment.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source");
        dataStreamSource.print();
        environment.execute("Print MySQL Snapshot + Binlog");
    }
}
