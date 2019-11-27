drop table user_groups;
create table user_groups (
  user_id integer not null references users(id),
  group_id smallint not null references groups(id),

  constraint user_groups_pk primary key(user_id, group_id)
);
