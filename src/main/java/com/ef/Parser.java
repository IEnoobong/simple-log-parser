package com.ef;

import com.ef.model.LogEntry;
import com.ef.repository.LogEntryRepository;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class Parser implements CommandLineRunner {

    private static final int ONE_HOUR = 1;
    private static final int TWENTY_FOUR_HOURS = 24;
    private static final String START_DATE = "startDate";
    private static final String THRESHOLD = "threshold";
    private static final String DURATION = "duration";
    private static final String LOG_FILE_PATH = "accesslog";
    private static final String START_DATE_FORMAT = "yyyy-MM-dd.HH:mm:ss";
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern(START_DATE_FORMAT);
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");


    private final LogEntryRepository accessRepository;

    @Autowired
    public Parser(LogEntryRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    public static void main(String... args) {
        SpringApplication.run(Parser.class, args);
    }

    private static LocalDateTime requireStartTime(String argStartDate) {
        try {
            return LocalDateTime.parse(argStartDate, INPUT_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    String.format("%s = %s must be in format %s", START_DATE, argStartDate, START_DATE_FORMAT));
        }
    }

    private static Path requireFilePath(String pathToAccessLog) {
        try {
            final Path path = Paths.get(pathToAccessLog);

            final boolean isRegularFile = Files.isRegularFile(path);
            final boolean isReadable = Files.isReadable(path);

            if (!isRegularFile || !isReadable) {
                throw new IllegalArgumentException(String.format("File path = %s is either not a regular file or is " +
                        "not readable", pathToAccessLog));
            }
            return path;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(String.format("File path = %s is invalid", pathToAccessLog));
        }
    }

    private static int requireDuration(String argDuration) {
        int duration;
        if (!"daily".equals(argDuration) && !"hourly".equals(argDuration)) {
            throw new IllegalArgumentException(String.format("duration = %s can only be hourly or daily", argDuration));
        } else if (argDuration.equals("daily")) {
            duration = TWENTY_FOUR_HOURS;
        } else {
            duration = ONE_HOUR;
        }
        return duration;
    }

    private static int getThreshold(String argThreshold) {
        try {
            return Integer.parseInt(argThreshold);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("threshold %s must be an int", argThreshold));
        }
    }

    @Override
    public void run(String... args) throws Exception {
        final Options options = new Options();

        options.addOption(START_DATE, true, "Date to start parsing from in yyyy-MM-dd.HH:mm:ss format");
        options.addOption(DURATION, true, "How far from start date. Could be hourly or daily");
        options.addOption(THRESHOLD, true, "number of logs that can be flagged as blocked. Must be an int");
        options.addOption(LOG_FILE_PATH, false, "path to log file to be read");

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);
        final String startDate = cmd.getOptionValue(START_DATE);
        final String duration = cmd.getOptionValue(DURATION);
        final String threshold = cmd.getOptionValue(THRESHOLD);
        final String logFilePath = cmd.getOptionValue(LOG_FILE_PATH);

        parse(startDate, duration, threshold, logFilePath);
    }

    public void parse(String argStartDate, String argDuration, String argThreshold, String logFilePath) {
        final int duration = requireDuration(argDuration);

        final int threshold = getThreshold(argThreshold);

        final LocalDateTime startTime = requireStartTime(argStartDate);
        final LocalDateTime endTime = startTime.plusHours(duration);

        if (logFilePath == null) {
            //default to resources
            final String pathToAccessLog = "access.log";
            final ClassLoader classLoader = this.getClass().getClassLoader();
            logFilePath = classLoader.getResource(pathToAccessLog).getFile();
        }

        try (Stream<String> lines = Files.lines(requireFilePath(logFilePath))) {
            lines.parallel().map(line -> {
                final StringTokenizer tokenizer = new StringTokenizer(line, "|");
                LogEntry logEntry = null;
                while (tokenizer.hasMoreTokens()) {
                    logEntry = new LogEntry(LocalDateTime.parse(tokenizer.nextToken(), LOG_FORMATTER),
                            tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
                }
                return logEntry;
            }).filter(Objects::nonNull).filter(logEntry -> {
                final LocalDateTime logTime = logEntry.getLogTime();
                return !logTime.isBefore(startTime) && !logTime.isAfter(endTime);
            }).collect(Collectors.groupingBy(LogEntry::getIpAddress, Collectors.toList()))
                    .entrySet().stream().parallel().forEach(stringListEntry -> {
                final List<LogEntry> logEntries = stringListEntry.getValue();
                if (logEntries.size() > threshold) {
                    System.out.println(stringListEntry.getKey() + " = " + logEntries);
                }
                accessRepository.saveAll(logEntries);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
