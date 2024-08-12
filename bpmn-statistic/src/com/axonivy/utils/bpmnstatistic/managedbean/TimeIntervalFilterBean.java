package com.axonivy.utils.bpmnstatistic.managedbean;

import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.CURRENT;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.POINT_SELECTIONS;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.RANGE_SELECTIONS;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.TODAY;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.WITH_IN_SELECTIONS;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalUnit.WEEK;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType;
import com.axonivy.utils.bpmnstatistic.enums.TimeIntervalUnit;
import com.axonivy.utils.bpmnstatistic.utils.DateUtils;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class TimeIntervalFilterBean implements Serializable {

  private static final long serialVersionUID = 6644524793047050080L;

  private static final String FILTER_DATA_BY_INTERVAL_RC_PARAMS_PATTERN = "[{name:'from', value:'%s'}, {name:'to', value:'%s'}]";
  private static final String FILTER_DATA_BY_INTERVAL_RC = "filterDataByIntervalRC(%s);";
  private static final String PROCESS_TIME_INTERVAL_ID = "monitor-form:process-time-interval";
  private static final String CURRENT_TIME_URL = "/Labels/This";

  private TimeIntervalType selectedType;
  private TimeIntervalUnit selectedUnit;
  private String selectedInterval;
  private String fromTime;
  private String toTime;
  private String currentTime;
  private Date fromDate;
  private Date toDate;
  private TimeIntervalFilter timeIntervalFilter;

  @PostConstruct
  public void initFilter() {
    timeIntervalFilter = new TimeIntervalFilter();
    selectedType = TODAY;
    selectedUnit = WEEK;
    onSelectIntervalType();
  }

  public void onSelectIntervalType() {
    timeIntervalFilter = new TimeIntervalFilter();
    if (selectedType == null) {
      fromTime = null;
      toTime = null;
      currentTime = null;
      return;
    }

    var today = LocalDate.now();
    if (isPointSelection()) {
      var formatter = new SimpleDateFormat(DateUtils.DATE_TIME_PATTERN);
      if (TODAY != selectedType) {
        today = today.minusDays(1);
      }
      var from = DateUtils.getDateFromLocalDate(today, null);
      timeIntervalFilter.setFrom(DateUtils.getDateFromLocalDate(today, null));
      fromTime = formatter.format(from);
      var to = DateUtils.getDateFromLocalDate(today, LocalTime.MAX);
      timeIntervalFilter.setTo(to);
      toTime = formatter.format(to);
    }
    if (isCurrentSelection()) {
      currentTime = Ivy.cms().co(CURRENT_TIME_URL);
      switch (selectedUnit) {
      case WEEK:
        timeIntervalFilter.setFrom(
            DateUtils.getDateFromLocalDate(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), null));
        timeIntervalFilter.setFrom(
            DateUtils.getDateFromLocalDate(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)), LocalTime.MAX));
        break;
      case MONTH:
        timeIntervalFilter
            .setFrom(DateUtils.getDateFromLocalDate(today.with(TemporalAdjusters.firstDayOfMonth()), null));
        timeIntervalFilter
            .setFrom(DateUtils.getDateFromLocalDate(today.with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX));
        break;
      case YEAR:
        timeIntervalFilter
            .setFrom(DateUtils.getDateFromLocalDate(today.with(TemporalAdjusters.firstDayOfYear()), null));
        timeIntervalFilter
            .setFrom(DateUtils.getDateFromLocalDate(today.with(TemporalAdjusters.lastDayOfYear()), LocalTime.MAX));
        break;
      default:
        break;
      }
    }
    if (isWithInSelection()) {
      if (Ivy.cms().co(CURRENT_TIME_URL).equals(currentTime)) {
        currentTime = null;
      }
    }

    updateDataOnChangingFilter();
  }

  public void onSelectUnit() {
    if (selectedUnit == null) {
      currentTime = null;
    }
    if (isCurrentSelection() && selectedUnit != null) {
      currentTime = Ivy.cms().co(CURRENT_TIME_URL);
    }
    updateDataOnChangingFilter();
  }

  private void updateDataOnChangingFilter() {
    if (timeIntervalFilter == null || timeIntervalFilter.getFrom() == null || timeIntervalFilter.getTo() == null) {
      return;
    }

    var formatter = new SimpleDateFormat(DateUtils.DATE_TIME_PATTERN);
    var params = String.format(FILTER_DATA_BY_INTERVAL_RC_PARAMS_PATTERN,
        formatter.format(timeIntervalFilter.getFrom()), formatter.format(timeIntervalFilter.getTo()));
    var script = FILTER_DATA_BY_INTERVAL_RC.formatted(params);
    PF.current().executeScript(script);
  }

  public void onChangeCalendarValue() {
    if (fromDate == null || toDate == null) {
      return;
    }
    if (fromDate.after(toDate)) {
      FacesContext.getCurrentInstance().validationFailed();
      FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "From must be before To",
          "From must be before To");
      FacesContext.getCurrentInstance().addMessage(PROCESS_TIME_INTERVAL_ID, message);
    }
  }

  public List<TimeIntervalType> getIntervalTypes() {
    return List.of(TimeIntervalType.values());
  }

  public List<TimeIntervalUnit> getIntervalUnits() {
    return List.of(TimeIntervalUnit.values());
  }

  public String getSelectedInterval() {
    return selectedInterval;
  }

  public Date getFromDate() {
    return fromDate;
  }

  public void setFromDate(Date fromDate) {
    this.fromDate = fromDate;
  }

  public Date getToDate() {
    return toDate;
  }

  public void setToDate(Date toDate) {
    this.toDate = toDate;
  }

  public void setSelectedInterval(String selectedInterval) {
    this.selectedInterval = selectedInterval;
  }

  public TimeIntervalType getSelectedType() {
    return selectedType;
  }

  public void setSelectedType(TimeIntervalType selectedType) {
    this.selectedType = selectedType;
  }

  public String getFromTime() {
    return fromTime;
  }

  public void setFromTime(String fromTime) {
    this.fromTime = fromTime;
  }

  public String getToTime() {
    return toTime;
  }

  public void setToTime(String toTime) {
    this.toTime = toTime;
  }

  public String getCurrentTime() {
    return currentTime;
  }

  public TimeIntervalUnit getSelectedUnit() {
    return selectedUnit;
  }

  public void setSelectedUnit(TimeIntervalUnit selectedUnit) {
    this.selectedUnit = selectedUnit;
  }

  public void setCurrentTime(String currentTime) {
    this.currentTime = currentTime;
  }

  public boolean isPointSelection() {
    return POINT_SELECTIONS.contains(selectedType);
  }

  public boolean isRangeSelection() {
    return RANGE_SELECTIONS.contains(selectedType);
  }

  public boolean isWithInSelection() {
    return WITH_IN_SELECTIONS.contains(selectedType);
  }

  public boolean isCurrentSelection() {
    return CURRENT.equals(selectedType);
  }
}
