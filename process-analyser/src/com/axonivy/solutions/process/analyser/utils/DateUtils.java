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

  private DateUtils() {
  }

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
    if (durationSeconds > 23 * 3600) {
      float days = durationSeconds / (24 * 3600);
      result = formatFloat(days) + "d";
    } else if (durationSeconds > 59 * 60) {
      float hours = durationSeconds / 3600;
      result = formatFloat(hours) + "h";
    } else if (durationSeconds > 59) {
      float minutes = durationSeconds / 60;
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
