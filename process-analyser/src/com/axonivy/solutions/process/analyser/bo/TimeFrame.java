package com.axonivy.solutions.process.analyser.bo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TimeFrame {
  private Date start;
  private Date end;
  
  public TimeFrame() {}

  public TimeFrame(Date start, Date end) {
    this.start = start;
    this.end = end;
  }

  public Date getStart() {
    return start;
  }

  public void setStart(Date start) {
    this.start = start;
  }

  public Date getEnd() {
    return end;
  }

  public void setEnd(Date end) {
    this.end = end;
  }
}
