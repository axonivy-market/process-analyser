package com.axonivy.solutions.process.analyser.managedbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.bo.ElementDisplayName;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.core.internal.ProcessViewerBuilder;
import com.axonivy.solutions.process.analyser.core.util.ProcessElementUtils;

@ManagedBean
@ViewScoped
public class ProcessViewerBean implements Serializable {

  private static final long serialVersionUID = -2589140797903853427L;
  private String bpmnIframeSourceUrl;
  private Process selectedProcess;
  private String selectedStartElement;
  private String selectedItem;
  private Integer selectedZoom = 100;
  private List<ElementDisplayName> availableProcessElements;

  public void init(ProcessAnalyser processAnalyser) {
    if (processAnalyser == null) {
      return;
    }
    selectedProcess = processAnalyser.getProcess();
    selectedStartElement = processAnalyser.getStartElement().getPid();
    availableProcessElements = new ArrayList<>();
    if (selectedProcess != null && StringUtils.isNoneBlank(selectedStartElement)) {
      unifySelectionData();
      updateBpmnIframeSourceUrl(selectedStartElement);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.PROCESS_ANALYTIC_VIEWER_PANEL);
    }
  }

  private void unifySelectionData() {
    availableProcessElements = ProcessElementUtils.listAllProcessElementAsRawPID(selectedProcess.getPmv(),
        selectedProcess.getId(), selectedStartElement);
    selectedItem = selectedStartElement;
  }

  public boolean shouldRenderBpmnFrame() {
    return StringUtils.isNoneBlank(bpmnIframeSourceUrl);
  }

  public void updateBpmnIframeSourceUrl(String selectedItem) {
    var builder = new ProcessViewerBuilder(selectedProcess);
    if (StringUtils.isNoneBlank(selectedItem)) {
      builder.select(selectedItem);
    }
    if (selectedZoom != null && selectedZoom > 0) {
      builder.zoom(selectedZoom);
    }
    bpmnIframeSourceUrl = builder.toURI().toString();
  }

  public void refreshBpmnIFrame() {
    updateBpmnIframeSourceUrl(selectedItem);
    PF.current().executeScript(ProcessAnalyticsConstants.UPDATE_IFRAME_SOURCE_METHOD_CALL);
  }

  public void resetViewerSelection() {
    selectedItem = null;
    selectedZoom = null;
    refreshBpmnIFrame();
  }

  public String getBpmnIframeSourceUrl() {
    return bpmnIframeSourceUrl;
  }

  public void setBpmnIframeSourceUrl(String bpmnIframeSourceUrl) {
    this.bpmnIframeSourceUrl = bpmnIframeSourceUrl;
  }

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(String selectedItem) {
    this.selectedItem = selectedItem;
  }

  public Integer getSelectedZoom() {
    return selectedZoom;
  }

  public void setSelectedZoom(Integer selectedZoom) {
    this.selectedZoom = selectedZoom;
  }

  public List<ElementDisplayName> getAvailableProcessElements() {
    return availableProcessElements;
  }

  public void setAvailableProcessElements(List<ElementDisplayName> availableProcessElements) {
    this.availableProcessElements = availableProcessElements;
  }
}
