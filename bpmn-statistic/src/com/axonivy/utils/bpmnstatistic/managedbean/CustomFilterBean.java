package com.axonivy.utils.bpmnstatistic.managedbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean
@ViewScoped
public class CustomFilterBean implements Serializable {

  private static final long serialVersionUID = 2227163631959126943L;

  private List<String> customFilters = new ArrayList<>();
  
  @PostConstruct
  public void initFilter() {
  }

  public List<String> getCustomFilters() {
    return customFilters;
  }

  public void setCustomFilters(List<String> customFilters) {
    this.customFilters = customFilters;
  }
}
