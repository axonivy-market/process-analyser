package com.axonivy.solutions.process.analyser.bo;

import java.util.List;
import java.util.Objects;

import ch.ivyteam.ivy.workflow.custom.field.ICustomFieldMeta;

public class CustomFieldFilter {
  private ICustomFieldMeta customFieldMeta;
  private boolean isCustomFieldFromCase;
  private List<Object> customFieldValues;

  public CustomFieldFilter(ICustomFieldMeta customFieldMeta, boolean isCustomFieldFromCase,
      List<Object> customFieldValues) {
    this.customFieldMeta = customFieldMeta;
    this.isCustomFieldFromCase = isCustomFieldFromCase;
    this.customFieldValues = customFieldValues;
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
