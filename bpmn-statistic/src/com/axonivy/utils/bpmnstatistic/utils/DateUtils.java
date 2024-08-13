package com.axonivy.utils.bpmnstatistic.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;

import ch.ivyteam.ivy.environment.Ivy;

public class DateUtils {

  public static final String TIME_PATTERN = "HH:mm";
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd " + TIME_PATTERN;
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

  public static String getDefaultMonthShortName(LocalDate fromDateTime) {
    return fromDateTime.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
  }

  public static String getDefaultMonthFullName(LocalDate fromDateTime) {
    return fromDateTime.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
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
