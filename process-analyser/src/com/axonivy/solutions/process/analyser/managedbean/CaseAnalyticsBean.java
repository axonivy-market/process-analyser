package com.axonivy.solutions.process.analyser.managedbean;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.solutions.process.analyser.utils.FacesContexts;

@ManagedBean
@ViewScoped
public class CaseAnalyticsBean {
  private ProcessesAnalyticsBean processesAnalyticsBean;

  @PostConstruct
  private void init() {
    processesAnalyticsBean = FacesContexts.evaluateValueExpression("#{processesAnalyticsBean}", ProcessesAnalyticsBean.class);
  }

  public void loadProcessViewerByCase() {
      processesAnalyticsBean.loadProcessViewerByCase();
  }
}
