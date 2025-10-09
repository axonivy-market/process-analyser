package com.axonivy.solutions.process.analyser.managedbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PF;

import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.constants.ProcessAnalyticViewComponentId;
import com.axonivy.solutions.process.analyser.core.bo.ElementDisplayName;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;
import com.axonivy.solutions.process.analyser.core.internal.ProcessViewerBuilder;
import com.axonivy.solutions.process.analyser.core.util.ProcessElementUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ICase;

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
  private ProcessAnalyser processAnalyser;
  private ICase selectedCase;

  public void init(ProcessAnalyser processAnalyser, Long caseId) {
    if (processAnalyser == null && (caseId == null || caseId == 0)) {
      return;
    }

    if (caseId != null && caseId != 0) {
      initByCaseId(caseId);
    } else  {
      initByProcessAnalyser(processAnalyser);
    }

    availableProcessElements = new ArrayList<>();
    if (selectedProcess != null) {
      unifySelectionData();
      updateBpmnIframeSourceUrl(selectedStartElement);
      PF.current().ajax().update(ProcessAnalyticViewComponentId.PROCESS_ANALYTIC_VIEWER_PANEL);
    }
  }

  private void initByProcessAnalyser(ProcessAnalyser processAnalyser) {
    selectedProcess = processAnalyser.getProcess();
    selectedStartElement = (processAnalyser.getStartElement() != null) ? processAnalyser.getStartElement().getPid() : null;
  }

  private void initByCaseId(Long caseId) {
    selectedCase = Ivy.wf().findCase(caseId);
    if (selectedCase != null) {
      var processStart = selectedCase.getProcessStart();
      var process =
          ProcessUtils.getProcessByPMVAndProcessStartElementId(selectedCase.getProcessModelVersion(), processStart.getProcessElementId());
      StartElement startElement = ProcessUtils.convertToStartELement(selectedCase.getProcessStart());
      process.getStartElements().add(startElement);
      selectedProcess = process;
      selectedStartElement = processStart.getProcessElementId();
      processAnalyser = new ProcessAnalyser(selectedProcess, startElement);
    }
  }

  private void unifySelectionData() {
    List<ElementDisplayName> elementDisplayNames = ProcessElementUtils.listAllProcessElementAsRawPID(selectedProcess.getPmv(),
        selectedProcess.getId(), selectedStartElement);
    availableProcessElements = createFriendlyNameForElement(elementDisplayNames);
    selectedItem = selectedStartElement;
  }

  private List<ElementDisplayName> createFriendlyNameForElement(List<ElementDisplayName> elementDisplayNames) {
    final var cmsPattern = "/Enums/ElementType/%s/name";
    CollectionUtils.emptyIfNull(elementDisplayNames).forEach(element -> {
      String cmsURL = cmsPattern.formatted(element.getElementType().name());
      element.setDisplayName(Ivy.cms().co(cmsURL, List.of(element.getDisplayName())));
    });
    return elementDisplayNames;
  }

  public boolean shouldRenderBpmnFrame() {
    return StringUtils.isNoneBlank(bpmnIframeSourceUrl);
  }

  public void updateBpmnIframeSourceUrl(String selectedItem) {
    var builder = new ProcessViewerBuilder();
    builder.pmv(selectedProcess.getPmvName());
    builder.projectPath(selectedProcess.getProjectRelativePath());
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

  public ProcessAnalyser getProcessAnalyser() {
    return processAnalyser;
  }

  public void setProcessAnalyser(ProcessAnalyser processAnalyser) {
    this.processAnalyser = processAnalyser;
  }

  public ICase getSelectedCase() {
    return selectedCase;
  }

  public void setSelectedCase(ICase selectedCase) {
    this.selectedCase = selectedCase;
  }
}
