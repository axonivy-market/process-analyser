package com.axonivy.utils.bpmnstatistic.managedbean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean
@ViewScoped
public class CustomFilterBean implements Serializable {

  private static final long serialVersionUID = 2227163631959126943L;

  private Map<String,List<String>> selectedCustomFilters = new HashMap<>();
  
  @PostConstruct
  public void initFilter() {
  }

  public Map<String, List<String>> getSelectedCustomFilters() {
    return selectedCustomFilters;
  }

  public void setSelectedCustomFilters(Map<String, List<String>> selectedCustomFilters) {
    this.selectedCustomFilters = selectedCustomFilters;
  }
}
