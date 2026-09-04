package com.axonivy.solutions.process.analyser.demo.managedbean;

import java.io.Serializable;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;

import org.apache.commons.lang3.StringUtils;

@Named
@ViewScoped
public class RecommendDestinationBean implements Serializable {
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
