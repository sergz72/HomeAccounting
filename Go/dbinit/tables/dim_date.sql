CREATE TABLE Dim_Date (
	date_id integer NOT NULL primary key,
	Date_Date date NOT NULL unique,
	date_string char(10) NOT NULL unique,
	day smallint NOT NULL,
	day_of_week smallint NOT NULL,
	week_id smallint NOT NULL,
	week5 char(35) NOT NULL,
	week7 char(35) NOT NULL,
	month_id int NOT NULL,
	month char(10) NOT NULL,
	quarter_id int NOT NULL,
	quarter char(10) NOT NULL,
	semester_id int NOT NULL,
	semester char(10) NOT NULL,
	year smallint NOT NULL
);
