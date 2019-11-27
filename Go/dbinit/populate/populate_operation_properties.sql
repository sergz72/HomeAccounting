insert into operation_properties(code, name, data_type, need_recalc)
values('SECA', 'Второй счет', 'dropdown:get_accounts', true);
insert into operation_properties(code, name, data_type) values('PPTO', 'Кому', 'dropdown:get_people');
insert into operation_properties(code, name, data_type) values('FROM', 'От кого', 'dropdown:get_people');
insert into operation_properties(code, name, data_type) values('COMM', 'Комментарий', 'string');
insert into operation_properties(code, name, data_type) values('FLAT', 'Квартира', 'dropdown:get_flats');
insert into operation_properties(code, name, data_type) values('SUMM', 'Сумма', 'numeric');
insert into operation_properties(code, name, data_type) values('AMOU', 'Количество', 'numeric');
insert into operation_properties(code, name, data_type) values('DATE', 'Дата', 'date');
insert into operation_properties(code, name, data_type) values('DTFR', 'Дата с', 'date');
insert into operation_properties(code, name, data_type) values('DTTO', 'Дата по', 'date');
insert into operation_properties(code, name, data_type) values('MODL', 'Марка', 'string');
insert into operation_properties(code, name, data_type) values('DIST', 'Показания одометра', 'numeric');
insert into operation_properties(code, name, data_type) values('NETW', 'Сеть', 'string');
insert into operation_properties(code, name, data_type) values('TYPE', 'Тип', 'string');
