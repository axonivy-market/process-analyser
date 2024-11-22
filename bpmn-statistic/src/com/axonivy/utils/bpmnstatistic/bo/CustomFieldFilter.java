package com.axonivy.utils.bpmnstatistic.bo;

import java.util.Objects;

import ch.ivyteam.ivy.workflow.custom.field.ICustomFieldMeta;

public class CustomFieldFilter {
  private ICustomFieldMeta customFieldMeta;
  private boolean isCustomFieldFromCase;

  public CustomFieldFilter(ICustomFieldMeta customFieldMeta, boolean isCustomFieldFromCase) {
    this.customFieldMeta = customFieldMeta;
    this.isCustomFieldFromCase = isCustomFieldFromCase;
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

  @Override
  public boolean equals(Object obj) {
    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }
    CustomFieldFilter that = (CustomFieldFilter) obj;
    return isCustomFieldFromCase == that.isCustomFieldFromCase && Objects.equals(customFieldMeta, that.customFieldMeta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customFieldMeta, isCustomFieldFromCase);
  }
}
