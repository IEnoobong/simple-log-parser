package com.ef;

import com.ef.model.LogEntry;
import com.ef.repository.LogEntryRepository;
import java.io.IOException;
import java.nio.file.Files;
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
import javax.annotation.PreDestroy;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Parser implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

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
    final Path path = Paths.get(pathToAccessLog);

    final boolean isRegularFile = Files.isRegularFile(path);
    final boolean isReadable = Files.isReadable(path);

    if (!isRegularFile || !isReadable) {
      throw new IllegalArgumentException(String.format("File path = %s is either not a regular file or is "
              + "not readable", pathToAccessLog));
    }
    return path;
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
  public void run(String... args) {
    final Options options = new Options();

    options.addRequiredOption("s", START_DATE, true, "Date to start parsing from in yyyy-MM-dd.HH:mm:ss format");
    options.addRequiredOption("d", DURATION, true, "How far from start date. Could be hourly or daily");
    options.addRequiredOption("t", THRESHOLD, true, "number of logs that can be flagged as blocked. Must be an int");
    options.addRequiredOption("p", LOG_FILE_PATH, true, "path to log file to be read");

    final CommandLineParser parser = new DefaultParser();
    final HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException ex) {
      formatter.printHelp("parser", options);
      return;
    }

    final String startDate = cmd.getOptionValue(START_DATE);
    final String duration = cmd.getOptionValue(DURATION);
    final String threshold = cmd.getOptionValue(THRESHOLD);
    final String logFilePath = cmd.getOptionValue(LOG_FILE_PATH);

    parse(startDate, duration, threshold, logFilePath);
  }

  private void parse(String argStartDate, String argDuration, String argThreshold, String logFilePath) {
    final int duration = requireDuration(argDuration);

    final int threshold = getThreshold(argThreshold);

    final LocalDateTime startTime = requireStartTime(argStartDate);
    final LocalDateTime endTime = startTime.plusHours(duration);

    LOG.info("The following IP(s)  made more than {} requests within the given time period", threshold);
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
              .entrySet().stream().parallel()
              .filter(stringListEntry -> stringListEntry.getValue().size() > threshold)
              .forEach(stringListEntry -> {
                final List<LogEntry> logEntries = stringListEntry.getValue();
                LOG.info(stringListEntry.getKey());
                accessRepository.saveAll(logEntries);
              });
    } catch (IOException e) {
      LOG.error("Error occurred", e);
    }
  }

  @PreDestroy
  public void cleanup() {
    LOG.info("cleaning up");
//    accessRepository.deleteAll();
  }
}
