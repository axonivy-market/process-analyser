package com.axonivy.utils.process.analyser.utils;

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
}
