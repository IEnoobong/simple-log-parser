# Web Server Access Log Parser

Parses a web server access log file and loads the log to MySQL database and checks if a given IP makes more than a 
certain number of requests for the given duration. 

## Usage
1. git clone and switch to directory
2. set your `spring.datasource.username` and `spring.datasource.password` in `applications.properties`
3. run `mvn spring-boot:run` in your terminal

Database scheme: logparser.sql
Sample Queries: queries.sql
Jar file: parser.jar