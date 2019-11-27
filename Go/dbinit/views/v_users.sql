drop view v_users;
create view v_users
as
select a.id user_id, a.name user_name, a.username login, c.id group_id, c.name group_name
  from user_groups b, users a, groups c
 where a.id = b.user_id and c.id = b.group_id and a.enabled = true
;
