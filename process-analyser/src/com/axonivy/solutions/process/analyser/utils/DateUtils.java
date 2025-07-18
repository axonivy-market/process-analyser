package com.axonivy.solutions.process.analyser.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class DateUtils {

  public static final String TIME_PATTERN = "HH:mm";
  public static final String DATE_PATTERN = "dd/MM/yyyy";
  public static final String DATE_TIME_PATTERN = DATE_PATTERN + StringUtils.SPACE + TIME_PATTERN;
  private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
  private static final int SECONDS_PER_HOUR = 3600;
  private static final int SECONDS_PER_MINUTE = 60;
  private static final int FIFTY_NINE_MINUTES = 59;
  private static final int TWENTY_THREE_HOURS = 23;
  private static final int HOURS_PER_DAY = 24;

  private DateUtils() {}

  public static Date getDateFromLocalDate(LocalDate localDate, LocalTime localTime) {
    if (localTime == null) {
      localTime = LocalTime.MIN;
    }
    var localDateTime = LocalDateTime.of(localDate, localTime);
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  public static String getDateAsString(Date date) {
    return simpleDateFormat.format(date);
  }

  public static Date parseDateFromString(String source) {
    try {
      return simpleDateFormat.parse(source);
    } catch (ParseException e) {
      Ivy.log().error("Cannot parse the source {0} to date", source);
      e.printStackTrace();
    }
    return null;
  }

  public static String convertDuration(float durationSeconds) {
    String result = StringUtils.EMPTY;
    if (durationSeconds > TWENTY_THREE_HOURS * SECONDS_PER_HOUR) {
      float days = durationSeconds / (HOURS_PER_DAY * SECONDS_PER_HOUR);
      result = formatFloat(days) + "d";
    } else if (durationSeconds > FIFTY_NINE_MINUTES * SECONDS_PER_MINUTE) {
      float hours = durationSeconds / SECONDS_PER_HOUR;
      result = formatFloat(hours) + "h";
    } else if (durationSeconds > FIFTY_NINE_MINUTES) {
      float minutes = durationSeconds / SECONDS_PER_MINUTE;
      result = formatFloat(minutes) + "m";
    } else {
      result = formatFloat(durationSeconds) + "s";
    }
    return result;
  }

  private static String formatFloat(float value) {
    return String.valueOf((long) Math.ceil(value));
  }
}
