CREATE PROCEDURE Fill_Dim_Date
	@start_date date,
	@end_date date
as
SET DATEFIRST 1

DECLARE @date date,
	@week_end5_str varchar(35)
	,@week_end7_str varchar(35)
	,@month varchar(10)
	,@semester char(10) 
	,@date_string varchar(10) 
	,@day_of_week tinyint 
	,@week_id tinyint 
	,@day int
	,@month_id int
	,@year int
	,@quarter int
	,@quarter_id int
	,@semester_id int
	,@date_Date as varchar(10)
	,@date_id int
	

DECLARE @row_count int, @sql Nvarchar(4000)

SET @row_count = 0
set @date = @start_date

WHILE @date <= @end_date
BEGIN

	select @month = DATENAME(m, @date)
	select @semester = 
		CASE 
			WHEN @date <= CAST(STR(YEAR(@date), 4, 0) + '-Jun-30' as datetime) THEN 1
			ELSE 2
		END 

	set @day = DAY(@date)
	set @year = YEAR(@date)
	set @month_id = (@year * 100)+ MONTH(@date)
	set @quarter = DATEPART(qq,@date)
	set @quarter_id  = (@year * 100)+ @quarter
	set @semester_id = (@year * 100)+ @semester
	set @date_id = ((@year * 100) + month(@date))* 100 + day(@date)

	declare @week_start datetime, @week_end5 datetime, @week_end7 datetime
	select  @week_start = dateadd(day, -DATEPART(dw, @date)+1, @date)
	select @week_end5 = @week_start + 4
	select @week_end7 = @week_start + 6

	select @week_end5_str = SUBSTRING(DATENAME(dw, @week_start), 1, 3) + ' ' + REPLACE(STR(MONTH(@week_start), 2,0) + '/' + STR(DAY(@week_start), 2,0) + '/' + STR(YEAR(@week_start), 4,0), ' ', '0') + ' - ' + 
		SUBSTRING(DATENAME(dw, @week_end5), 1, 3) + ' ' + REPLACE(STR(MONTH(@week_end5), 2,0) + '/' + STR(DAY(@week_end5), 2,0) + '/' + STR(YEAR(@week_end5), 4,0), ' ', '0')

	select @week_end7_str = SUBSTRING(DATENAME(dw, @week_start), 1, 3) + ' ' + REPLACE(STR(MONTH(@week_start), 2,0) + '/' + STR(DAY(@week_start), 2,0) + '/' + STR(YEAR(@week_start), 4,0), ' ', '0') + ' - ' + 
		SUBSTRING(DATENAME(dw, @week_end7), 1, 3) + ' ' + REPLACE(STR(MONTH(@week_end7), 2,0) + '/' + STR(DAY(@week_end7), 2,0) + '/' + STR(YEAR(@week_end7), 4,0), ' ', '0')

	select @date_string = REPLACE(STR(MONTH(@date), 2, 0) + '/' + STR(DAY(@date), 2,0) + '/' + STR(YEAR(@date), 4, 0),' ', '0')


	select @date_Date = convert(varchar(10),@date,101)

	select @month = DATENAME(m, @date)

	select @day_of_week = DATEPART(dw, @date)
	select @week_id = DATEPART(wk, @date)

	IF @day_of_week = 6 or @day_of_week = 7
		set @week_end5_str = ''

	select @sql = N'INSERT INTO [dbo].[Dim_Date]'+
		   '([date_id]' +
           ',[Date_Date]' +
		   ',[date_string]'+
           ',[day]'+
           ',[day_of_week]'+
           ',[week_id]'+
           ',[week5]'+
           ',[week7]'+
           ',[month_id]'+
           ',[month]'+
			',[quarter_id]'+
           ',[quarter]'+
			',[semester_id]'+
           ',[semester]'+
           ',[year])'+	
     'VALUES'+
           '(''' + str(@date_id,8,0)  +
		   ''',''' + @date_Date +
		   ''',''' + @date_string +
           ''',' + STR(@day, 2, 0) +
           ',' + STR(@day_of_week, 2, 0) + 
           ',' + STR(@week_id) +
           ',''' + @week_end5_str +
           ''',''' + @week_end7_str +
           ''',' + STR(@month_id, 6, 0) +
           ',''' + @month + 
		   ''',''' + str(@quarter_id,6,0) + 
           ''',' + STR(@quarter, 2, 0) +
		   ',' + STR(@semester_id, 6, 0) +
           ',' + STR(@semester, 2, 0) +
           ',' + STR(@year, 4, 0) + ')'

	exec sp_executesql @sql


	set @date = dateadd(day, 1, @date)
	set @row_count = @row_count + 1
END

return @row_count
GO
