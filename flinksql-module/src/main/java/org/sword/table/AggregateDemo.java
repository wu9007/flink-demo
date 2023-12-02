package org.sword.table;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * @author chuan
 * @version 1.0
 * @since 2023/12/2
 */
public class AggregateDemo {

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

        //用户事件表
        String userActionTableSql = "create table t_user_action(" +
                "    action_id         VARCHAR," +
                "    user_id         VARCHAR," +
                "    user_name         VARCHAR," +
                "    action      VARCHAR," +
                "    action_time      TIMESTAMP(3)," +
                "    ip          VARCHAR," +
                "    device_name VARCHAR," +
                "    PRIMARY KEY (action_id) NOT ENFORCED" +
                ") WITH (" +
                " 'connector' = 'mysql-cdc'," +
                " 'hostname' = '192.168.2.104'," +
                " 'port' = '3306'," +
                " 'username' = 'root'," +
                " 'password' = 'root'," +
                " 'database-name' = 'flinkdb'," +
                " 'table-name' = 't_user_action'," +
                " 'scan.incremental.snapshot.enabled'='false'" +
                ")";
        streamTableEnvironment.executeSql(userActionTableSql);

        //每天的登录注册情况
        String dailyStatsTableSql = "create table dws_daily_stats(" +
                "    login_date          DATE," +
                "    login_count         BIGINT," +
                "    register_count      BIGINT," +
                "    PRIMARY KEY (login_date) NOT ENFORCED" +
                ") WITH (" +
                " 'connector' = 'jdbc'," +
                " 'url' = 'jdbc:mysql://192.168.2.104:3306/flinkdb?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=false'," +
                " 'table-name' = 'dws_daily_stats'," +
                " 'username' = 'root'," +
                " 'password' = 'root'," +
                " 'driver' = 'com.mysql.cj.jdbc.Driver'" +
                ")";
        streamTableEnvironment.executeSql(dailyStatsTableSql);

        String aggregateSql = "INSERT INTO dws_daily_stats " +
                "SELECT " +
                "    CAST(action_time AS DATE)             AS login_date, " +
                "    COUNT(IF(action = '$login', 1, 0))    AS login_count, " +
                "    COUNT(IF(action = '$register', 1, 0)) AS register_count " +
                "FROM t_user_action " +
                "GROUP BY CAST(action_time AS DATE) ";
        streamTableEnvironment.executeSql(aggregateSql);
    }
}
