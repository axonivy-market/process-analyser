package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.enums.TimeIntervalType.POINT_SELECTIONS;
import static com.axonivy.solutions.process.analyser.enums.TimeIntervalType.TODAY;
import static com.axonivy.solutions.process.analyser.enums.TimeIntervalType.YESTERDAY;
import static com.axonivy.solutions.process.analyser.enums.TimeIntervalType.CUSTOM;
import static com.axonivy.solutions.process.analyser.enums.TimeIntervalType.BETWEEN;
import static com.axonivy.solutions.process.analyser.utils.DateUtils.DATE_PATTERN;
import static com.axonivy.solutions.process.analyser.utils.DateUtils.DATE_TIME_PATTERN;
import static com.axonivy.solutions.process.analyser.utils.DateUtils.getDateAsString;
import static com.axonivy.solutions.process.analyser.utils.DateUtils.getDateFromLocalDate;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.TimeIntervalFilter;
import com.axonivy.solutions.process.analyser.enums.TimeIntervalType;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class TimeIntervalFilterBean implements Serializable {

  private static final long serialVersionUID = 6644524793047050080L;

  private static final String FILTER_DATA_BY_INTERVAL_RC_PARAMS_PATTERN =
      "[{name:'from', value:'%s'}, {name:'to', value:'%s'}]";
  private static final String FILTER_DATA_BY_INTERVAL_RC = "filterDataByIntervalRC(%s);";
  private static final String CUSTOM_DATE_TO_ID = "process-analytics-form:custom-date-to";

  private TimeIntervalType selectedType;
  private String currentTime;
  private TimeIntervalFilter filter;
  private List<Date> selectedDateRange;

  @PostConstruct
  public void initFilter() {
    filter = new TimeIntervalFilter();
    selectedType = TODAY;
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

  public void onSelectDateTime() {
    if (filter == null || filter.getFrom() == null || filter.getTo() == null) {
      return;
    }
    if (filter.getFrom().after(filter.getTo())) {
      var message = new FacesMessage(FacesMessage.SEVERITY_ERROR, null,
          Ivy.cms().co("/Dialogs/com/axonivy/solutions/process/analyser/ProcessesMonitor/FromToDateValidationMessage"));
      FacesContext.getCurrentInstance().addMessage(CUSTOM_DATE_TO_ID, message);
      FacesContext.getCurrentInstance().validationFailed();
      return;
    }
    unifyFilterAndRefreshData();
  }

  public void onSelectDateRange() {
    filter.setFrom(selectedDateRange.get(0));
    filter.setTo(selectedDateRange.get(1));
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

  public List<TimeIntervalType> getIntervalTypes() {
    return List.of(TimeIntervalType.values());
  }

  public List<Date> getSelectedDateRange() {
    return selectedDateRange;
  }

  public void setSelectedDateRange(List<Date> selectedDateRange) {
    this.selectedDateRange = selectedDateRange;
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

  public void setCurrentTime(String currentTime) {
    this.currentTime = currentTime;
  }

  public boolean isPointSelection() {
    return POINT_SELECTIONS.contains(selectedType);
  }

  public boolean isCustomSelection() {
    return CUSTOM == selectedType;
  }

  public boolean isBetweenSelection() {
    return BETWEEN == selectedType;
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

  public String getDatePattern() {
    return DATE_PATTERN;
  }
}
