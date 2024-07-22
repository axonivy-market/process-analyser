package com.axonivy.utils.bpmnstatistic.managedbean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;

@ManagedBean
@ViewScoped
public class RecommendDestinationBean {
  private String from;
  private String to;
  private Integer rating;

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public boolean isCompleted() {
    return StringUtils.isNoneBlank(from, to) && rating != null;
  }
}
