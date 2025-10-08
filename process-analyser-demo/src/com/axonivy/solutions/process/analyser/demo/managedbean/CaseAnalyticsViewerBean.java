package com.axonivy.solutions.process.analyser.demo.managedbean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.solutions.process.analyser.enums.KpiType;

@ManagedBean
@ViewScoped
public class CaseAnalyticsViewerBean {

  private KpiType selectedKpiType;

  public KpiType[] getKpiTypes() {
    return KpiType.values();
  }

  public KpiType getSelectedKpiType() {
    return selectedKpiType;
  }

  public void setSelectedKpiType(KpiType selectedKpiType) {
    this.selectedKpiType = selectedKpiType;
  }
}
