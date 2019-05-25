# Web Server Access Log Parser

Parses a web server access log file and loads the log to MySQL database and checks if a given IP makes more than a 
certain number of requests for the given duration. 

## Usage
1. git clone and switch to directory
2. set your `spring.datasource.username` and `spring.datasource.password` in `applications.properties`
3. run `mvn spring-boot:run` in your terminal

**Database scheme**: schema.sql  
**SQL Queries**: queries.sql  
**Jar file**: parser.jar

## Notes
I couldn't get `java -cp "parser.jar" com.ef.Parser --startDate=2017-01-01.13:00:00 --duration=hourly 
--threshold=100` to work but jar can be run with `java -jar target/parser.jar -duration hourly -threshold 200 -startDate 2017-01-01.15:00:00 -accesslog src/main/resources/access.log`
