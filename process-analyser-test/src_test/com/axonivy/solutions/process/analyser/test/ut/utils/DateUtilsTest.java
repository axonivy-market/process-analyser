package com.axonivy.solutions.process.analyser.test.ut.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.utils.DateUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class DateUtilsTest {

  @Test
  void testSecondsUnder60() {
    assertEquals("59s", DateUtils.convertDuration(59f));
    assertEquals("1s", DateUtils.convertDuration(0.1f));
    assertEquals("1s", DateUtils.convertDuration(1f));
  }

  @Test
  void testExactly60Seconds() {
    assertEquals("1m", DateUtils.convertDuration(60f));
  }

  @Test
  void testMinutesUnderHour() {
    assertEquals("2m", DateUtils.convertDuration(61f));
    assertEquals("1h", DateUtils.convertDuration(3599f));
  }

  @Test
  void testExactlyOneHour() {
    assertEquals("1h", DateUtils.convertDuration(3600f));
  }

  @Test
  void testHoursUnderDay() {
    assertEquals("2h", DateUtils.convertDuration(7200f));
    assertEquals("23h", DateUtils.convertDuration(82800f)); // 23 * 3600
  }

  @Test
  void testExactlyOneDay() {
    assertEquals("1d", DateUtils.convertDuration(86400f));
  }

  @Test
  void testDaysAboveThreshold() {
    assertEquals("2d", DateUtils.convertDuration(172800f));
    assertEquals("3d", DateUtils.convertDuration(200000f));
  }
}
