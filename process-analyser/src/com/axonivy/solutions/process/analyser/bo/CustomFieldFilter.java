package com.axonivy.solutions.process.analyser.bo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import ch.ivyteam.ivy.workflow.custom.field.ICustomFieldMeta;

public class CustomFieldFilter {
  private ICustomFieldMeta customFieldMeta;
  private boolean isCustomFieldFromCase;
  private List<Object> customFieldValues;
  private List<Object> availableCustomFieldValues;
  private List<LocalDate> timestampCustomFieldValues;

  public CustomFieldFilter(ICustomFieldMeta customFieldMeta, boolean isCustomFieldFromCase,
      List<Object> customFieldValues, List<LocalDate> timestampCustomFieldValues) {
    this.customFieldMeta = customFieldMeta;
    this.isCustomFieldFromCase = isCustomFieldFromCase;
    this.customFieldValues = customFieldValues;
    this.availableCustomFieldValues = customFieldValues;
    this.timestampCustomFieldValues = timestampCustomFieldValues;
  }

  public CustomFieldFilter() {}

  public ICustomFieldMeta getCustomFieldMeta() {
    return customFieldMeta;
  }

  public void setCustomFieldMeta(ICustomFieldMeta customFieldMeta) {
    this.customFieldMeta = customFieldMeta;
  }

  public boolean isCustomFieldFromCase() {
    return isCustomFieldFromCase;
  }

  public void setCustomFieldFromCase(boolean isCustomFieldFromCase) {
    this.isCustomFieldFromCase = isCustomFieldFromCase;
  }

  public List<Object> getCustomFieldValues() {
    return customFieldValues;
  }

  public void setCustomFieldValues(List<Object> customFieldValues) {
    this.customFieldValues = customFieldValues;
  }

  public List<Object> getAvailableCustomFieldValues() {
    return availableCustomFieldValues;
  }

  public void setAvailableCustomFieldValues(List<Object> availableCustomFieldValues) {
    this.availableCustomFieldValues = availableCustomFieldValues;
  }

  public List<LocalDate> getTimestampCustomFieldValues() {
    return timestampCustomFieldValues;
  }

  public void setTimestampCustomFieldValues(List<LocalDate> timestampCustomFieldValues) {
    this.timestampCustomFieldValues = timestampCustomFieldValues;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }
    CustomFieldFilter customFieldFilter = (CustomFieldFilter) obj;
    return isCustomFieldFromCase == customFieldFilter.isCustomFieldFromCase
        && Objects.equals(customFieldMeta, customFieldFilter.customFieldMeta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customFieldMeta, isCustomFieldFromCase);
  }
}
