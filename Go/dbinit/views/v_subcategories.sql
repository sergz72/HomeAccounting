drop view v_subcategories;
create view v_subcategories
as
select a.id, a.name subcategory, a.category_id, b.name category, a.group_id, c.name group_name
  from categories b
       inner join subcategories a on a.category_id = b.id
       left join subcategory_groups c on a.group_id = c.id
;
