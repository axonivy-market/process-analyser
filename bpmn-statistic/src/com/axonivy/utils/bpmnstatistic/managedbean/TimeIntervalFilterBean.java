package com.axonivy.utils.bpmnstatistic.managedbean;

import static com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants.COMMA_CONNECT_PATTERN;
import static com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants.CURRENT_TIME_PATTERN;
import static com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants.WHOLE_DAY_PATTERN;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.CURRENT;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.POINT_SELECTIONS;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.RANGE_SELECTIONS;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.TODAY;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.WITHIN_THE_NEXT;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.WITH_IN_SELECTIONS;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType.YESTERDAY;
import static com.axonivy.utils.bpmnstatistic.enums.TimeIntervalUnit.WEEK;
import static com.axonivy.utils.bpmnstatistic.utils.DateUtils.DATE_TIME_PATTERN;
import static com.axonivy.utils.bpmnstatistic.utils.DateUtils.TIME_PATTERN;
import static com.axonivy.utils.bpmnstatistic.utils.DateUtils.getDateAsString;
import static com.axonivy.utils.bpmnstatistic.utils.DateUtils.getDateFromLocalDate;
import static com.axonivy.utils.bpmnstatistic.utils.DateUtils.getDefaultMonthFullName;
import static com.axonivy.utils.bpmnstatistic.utils.DateUtils.getDefaultMonthShortName;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.PF;

import com.axonivy.utils.bpmnstatistic.bo.TimeIntervalFilter;
import com.axonivy.utils.bpmnstatistic.enums.TimeIntervalType;
import com.axonivy.utils.bpmnstatistic.enums.TimeIntervalUnit;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class TimeIntervalFilterBean implements Serializable {

  private static final long serialVersionUID = 6644524793047050080L;

  private static final String FILTER_DATA_BY_INTERVAL_RC_PARAMS_PATTERN = "[{name:'from', value:'%s'}, {name:'to', value:'%s'}]";
  private static final String FILTER_DATA_BY_INTERVAL_RC = "filterDataByIntervalRC(%s);";
  private static final String PROCESS_TIME_INTERVAL_ID = "monitor-form:process-time-interval";

  private TimeIntervalType selectedType;
  private TimeIntervalUnit selectedUnit;
  private String currentTime;
  private TimeIntervalFilter filter;

  @PostConstruct
  public void initFilter() {
    filter = new TimeIntervalFilter();
    selectedType = TODAY;
    selectedUnit = WEEK;
    unifyFilterAndRefreshData();
  }

  public void onSelectType() {
    if (selectedType == null) {
      filter = new TimeIntervalFilter();
      currentTime = null;
      return;
    }
    unifyFilterAndRefreshData();
  }

  public void onSelectUnit() {
    if (selectedUnit == null) {
      currentTime = null;
    }
    unifyFilterAndRefreshData();
  }

  public void onSelectDateTime() {
    if (filter == null || filter.getFrom() == null || filter.getTo() == null) {
      return;
    }
    if (filter.getFrom().after(filter.getTo())) {
      var message = new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
          Ivy.cms().co("/Dialogs/com/axonivy/utils/bpmnstatistic/ProcessesMonitor/FromToDateValidationMessage"));
      FacesContext.getCurrentInstance().addMessage(PROCESS_TIME_INTERVAL_ID, message);
      FacesContext.getCurrentInstance().validationFailed();
      return;
    }
    unifyFilterAndRefreshData();
  }

  public void unifyFilterAndRefreshData() {
    if (isPointSelection()) {
      var fromDateTime = LocalDate.now();
      var toDateTime = LocalDate.now();
      if (YESTERDAY == selectedType) {
        fromDateTime = fromDateTime.minusDays(1);
        toDateTime = toDateTime.minusDays(1);
      }
      filter.setFrom(getDateFromLocalDate(fromDateTime, null));
      filter.setTo(getDateFromLocalDate(toDateTime, LocalTime.MAX));
    }
    if (isCurrentSelection()) {
      calculateTimeByCurrentSelection();
    }
    if (isWithInSelection()) {
      calculateTimeByWithInSelection();
    }

    updateDataOnChangingFilter();
  }

  private void updateDataOnChangingFilter() {
    if (filter == null || filter.getFrom() == null || filter.getTo() == null) {
      return;
    }

    var params = String.format(FILTER_DATA_BY_INTERVAL_RC_PARAMS_PATTERN, getDateAsString(filter.getFrom()),
        getDateAsString(filter.getTo()));
    var script = FILTER_DATA_BY_INTERVAL_RC.formatted(params);
    PF.current().executeScript(script);
  }

  private void calculateTimeByWithInSelection() {
    var today = LocalDate.now();
    var fromDateTime = LocalDate.now();
    var toDateTime = LocalDate.now();
    if (!NumberUtils.isDigits(currentTime)) {
      currentTime = null;
    }
    var enteredCurrentTime = NumberUtils.toInt(currentTime);
    switch (selectedUnit) {
    case WEEK:
      today = WITHIN_THE_NEXT == selectedType ? today.plusWeeks(enteredCurrentTime)
          : today.minusWeeks(enteredCurrentTime);
      fromDateTime = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      toDateTime = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
      break;
    case MONTH:
      today = WITHIN_THE_NEXT == selectedType ? today.plusMonths(enteredCurrentTime)
          : today.minusMonths(enteredCurrentTime);
      fromDateTime = today.with(TemporalAdjusters.firstDayOfMonth());
      toDateTime = today.with(TemporalAdjusters.lastDayOfMonth());
      break;
    case YEAR:
      today = WITHIN_THE_NEXT == selectedType ? today.plusYears(enteredCurrentTime)
          : today.minusYears(enteredCurrentTime);
      fromDateTime = today.with(TemporalAdjusters.firstDayOfYear());
      toDateTime = today.with(TemporalAdjusters.lastDayOfYear());
      break;
    default:
      break;
    }
    filter.setFrom(getDateFromLocalDate(fromDateTime, null));
    filter.setTo(getDateFromLocalDate(toDateTime, LocalTime.MAX));
  }

  private void calculateTimeByCurrentSelection() {
    var today = LocalDate.now();
    var fromDateTime = LocalDate.now();
    var toDateTime = LocalDate.now();
    switch (selectedUnit) {
    case WEEK:
      fromDateTime = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      if (isSameDayOfMonth(fromDateTime, toDateTime)) {
        currentTime = getTimeAsWholeDayToday(toDateTime);
      } else {
        currentTime = String.format(CURRENT_TIME_PATTERN, fromDateTime.getDayOfMonth(), toDateTime.getDayOfMonth(),
            getDefaultMonthFullName(toDateTime));
      }
      break;
    case MONTH:
      fromDateTime = today.with(TemporalAdjusters.firstDayOfMonth());
      if (isSameDayOfMonth(fromDateTime, toDateTime)) {
        currentTime = getTimeAsWholeDayToday(toDateTime);
      } else {
        currentTime = String.format(CURRENT_TIME_PATTERN, fromDateTime.getDayOfMonth(), toDateTime.getDayOfMonth(),
            getDefaultMonthShortName(toDateTime)) + COMMA_CONNECT_PATTERN + toDateTime.getYear();
      }
      break;
    case YEAR:
      fromDateTime = today.with(TemporalAdjusters.firstDayOfYear());
      if (fromDateTime.getMonthValue() == toDateTime.getMonthValue()) {
        currentTime = getTimeAsWholeDayToday(toDateTime);
      } else {
        currentTime = String.format(CURRENT_TIME_PATTERN, getDefaultMonthShortName(fromDateTime),
            getDefaultMonthShortName(toDateTime), toDateTime.getYear());
      }
      break;
    default:
      break;
    }
    filter.setFrom(getDateFromLocalDate(fromDateTime, null));
    filter.setTo(getDateFromLocalDate(toDateTime, LocalTime.MAX));
  }

  private boolean isSameDayOfMonth(LocalDate fromDateTime, LocalDate toDateTime) {
    return fromDateTime.getDayOfMonth() == toDateTime.getDayOfMonth();
  }

  private String getTimeAsWholeDayToday(LocalDate toDateTime) {
    return String.format(WHOLE_DAY_PATTERN, LocalTime.MIN,
        LocalTime.now().format(DateTimeFormatter.ofPattern(TIME_PATTERN)), toDateTime.getDayOfMonth(),
        getDefaultMonthShortName(toDateTime), toDateTime.getYear());
  }

  public List<TimeIntervalType> getIntervalTypes() {
    return List.of(TimeIntervalType.values());
  }

  public List<TimeIntervalUnit> getIntervalUnits() {
    return List.of(TimeIntervalUnit.values());
  }

  public TimeIntervalType getSelectedType() {
    return selectedType;
  }

  public void setSelectedType(TimeIntervalType selectedType) {
    this.selectedType = selectedType;
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

  public TimeIntervalFilter getFilter() {
    return filter;
  }

  public void setFilter(TimeIntervalFilter filter) {
    this.filter = filter;
  }

  public String getTimePattern() {
    return DATE_TIME_PATTERN;
  }
}
