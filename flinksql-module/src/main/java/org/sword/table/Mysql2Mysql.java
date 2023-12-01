package org.sword.table;

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

        //事件表
        String actionTableSql = "create table t_action(" +
                "    action_id          VARCHAR," +
                "    user_id         VARCHAR," +
                "    action      VARCHAR," +
                "    action_time      TIMESTAMP(3)," +
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

        //用户信息表
        String userTableSql = "create table t_user(" +
                "    user_id          VARCHAR," +
                "    user_name         VARCHAR" +
                ") WITH (" +
                " 'connector' = 'mysql-cdc'," +
                " 'hostname' = '192.168.2.104'," +
                " 'port' = '3306'," +
                " 'username' = 'root'," +
                " 'password' = 'root'," +
                " 'database-name' = 'flinkdb'," +
                " 'table-name' = 't_user'," +
                " 'scan.incremental.snapshot.enabled'='false'" +
                ")";
        streamTableEnvironment.executeSql(userTableSql);
        //Join 表
        String resultTableSql = "create table t_user_action(" +
                "    action_id         VARCHAR," +
                "    user_id         VARCHAR," +
                "    user_name         VARCHAR," +
                "    action      VARCHAR," +
                "    action_time      TIMESTAMP(3)," +
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
                "SELECT t1.action_id, t1.user_id, t2.user_name, t1.action, t1.action_time, t1.ip, t1.device_name " +
                "FROM t_action t1 left join t_user t2 on t1.user_id = t2.user_id";
        streamTableEnvironment.executeSql(joinSql);
    }
}
