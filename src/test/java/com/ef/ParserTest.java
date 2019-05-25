package com.ef;

import com.ef.repository.LogEntryRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class ParserTest {

  private final LogEntryRepository logEntryRepository = mock(LogEntryRepository.class);

  private Parser parser = new Parser(logEntryRepository);

  private String[] args;
  private String[] ips;

  public ParserTest(String[] args, String[] ips) {
    this.args = args;
    this.ips = ips;
  }

  @Parameterized.Parameters
  public static List<String[][]> givenData() {
    final String logPath = "src/test/resources/access.log";
    final String[] dailyArgs = {"-duration", "daily", "-threshold", "500", "-startDate", "2017-01-01.00:00:00", "-accesslog",
            logPath};
    final String[] dailyIps = {"192.168.143.177", "192.168.31.26", "192.168.129.191", "192.168.52.153", "192.168.219.10",
            "192.168.203.111", "192.168.199.209", "192.168.38.77", "192.168.33.16", "192.168.162.248", "192.168.206.141",
            "192.168.102.136", "192.168.62.176", "192.168.51.205", "192.168.185.164"};

    final String[] hourlyArgs = {"-duration", "hourly", "-threshold", "200", "-startDate", "2017-01-01.15:00:00",
            "-accesslog", logPath};
    final String[] hourlyIps = {"192.168.11.231", "192.168.106.134"};


    return Arrays.asList(new String[][][]{
            {dailyArgs, dailyIps},
            {hourlyArgs, hourlyIps}
    });
  }

  @Test
  public void givenArgumentsShouldParseCorrectly() {
    parser.run(args);

    verify(logEntryRepository, times(ips.length)).saveAll(anyList());
  }

}
