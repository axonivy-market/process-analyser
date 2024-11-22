package com.axonivy.utils.process.analyzer.bo;

import static com.axonivy.utils.process.analyzer.utils.DateUtils.getDateFromLocalDate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

public class TimeIntervalFilter {
  private Date from;
  private Date to;

  public TimeIntervalFilter() {
  }

  public TimeIntervalFilter(Date from, Date to) {
    this.from = from;
    this.to = to;
  }

  public Date getFrom() {
    return from;
  }

  public void setFrom(Date from) {
    this.from = from;
  }

  public Date getTo() {
    return to;
  }

  public void setTo(Date to) {
    this.to = to;
  }

  @Override
  public String toString() {
    return "TimeIntervalFilter [from=" + from + ", to=" + to + "]";
  }

  public static TimeIntervalFilter getDefaultFilterSet() {
    var today = LocalDate.now();
    return new TimeIntervalFilter(getDateFromLocalDate(today, LocalTime.MIN),
        getDateFromLocalDate(today, LocalTime.MAX));
  }
}
