package org.example.table;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * @author chuan
 * @version 1.0
 * @since 2023/11/29
 */
public class Mysql2Mysql {

    public static void main(String[] args) {

        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(1);
        // enable checkpoint
        environment.enableCheckpointing(5000);
        environment.getCheckpointConfig().setCheckpointTimeout(10000);
        environment.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        environment.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        // 创建Table环境
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(environment, settings);

        String actionTableSql = "create table t_action(" +
                "    action_id          VARCHAR," +
                "    uid         VARCHAR," +
                "    action      VARCHAR," +
                "    ip          VARCHAR," +
                "    device_name VARCHAR" +
                ") WITH (" +
                " 'connector' = 'mysql-cdc'," +
                " 'hostname' = '192.168.2.104'," +
                " 'port' = '3306'," +
                " 'username' = 'root'," +
                " 'password' = 'root'," +
                " 'database-name' = 'flinkdb'," +
                " 'table-name' = 't_action'," +
                " 'scan.incremental.snapshot.enabled'='false'" +
                ")";
        streamTableEnvironment.executeSql(actionTableSql);
        //TODO 聚合
        String resultTableSql = "create table t_user_action(" +
                "    action_id          VARCHAR," +
                "    uid         VARCHAR," +
                "    action      VARCHAR," +
                "    ip          VARCHAR," +
                "    device_name VARCHAR," +
                "    PRIMARY KEY (action_id) NOT ENFORCED" +
                ") WITH (" +
                " 'connector' = 'jdbc'," +
                " 'url' = 'jdbc:mysql://192.168.2.104:3306/flinkdb?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=false'," +
                " 'table-name' = 't_user_action'," +
                " 'username' = 'root'," +
                " 'password' = 'root'," +
                " 'driver' = 'com.mysql.cj.jdbc.Driver'" +
                ")";
        streamTableEnvironment.executeSql(resultTableSql);

        String joinSql = "INSERT INTO t_user_action " +
                "SELECT * FROM t_action";
        streamTableEnvironment.executeSql(joinSql).print();
    }
}
