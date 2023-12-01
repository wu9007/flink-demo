create table flinkdb.t_action
(
    action_id   int auto_increment
        primary key,
    user_id     varchar(4)  null,
    action      varchar(20) null,
    action_time timestamp   null,
    ip          varchar(20) null,
    device_name varchar(50) null
);

create table flinkdb.t_user
(
    user_id   varchar(20) not null
        primary key,
    user_name varchar(50) not null
);

create table flinkdb.t_user_action
(
    action_id   varchar(20) not null
        primary key,
    user_id     varchar(20) null,
    user_name   varchar(50) null,
    action      varchar(20) not null,
    action_time timestamp   not null,
    ip          varchar(20) not null,
    device_name varchar(50) not null
);

