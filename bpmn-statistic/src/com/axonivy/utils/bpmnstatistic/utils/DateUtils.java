package com.axonivy.utils.bpmnstatistic.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class DateUtils {

  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

  private DateUtils() {
  }

  public static Date getDateFromLocalDate(LocalDate localDate, LocalTime localTime) {
    if (localTime == null) {
      localTime = LocalTime.MIN;
    }
    var localDateTime = LocalDateTime.of(localDate, LocalTime.MIN);
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
