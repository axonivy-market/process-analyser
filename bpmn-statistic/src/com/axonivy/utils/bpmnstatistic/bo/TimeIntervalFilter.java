package com.axonivy.utils.bpmnstatistic.bo;

import java.util.Date;

public class TimeIntervalFilter {
  private Date from;
  private Date to;

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
}
