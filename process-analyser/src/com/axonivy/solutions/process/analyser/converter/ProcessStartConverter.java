package com.axonivy.solutions.process.analyser.converter;

import java.util.List;
import java.util.Optional;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import com.axonivy.solutions.process.analyser.bo.ProcessAnalyser;
import com.axonivy.solutions.process.analyser.core.bo.Process;
import com.axonivy.solutions.process.analyser.core.bo.StartElement;
import com.axonivy.solutions.process.analyser.managedbean.MasterDataBean;
import com.axonivy.solutions.process.analyser.managedbean.ProcessesAnalyticsBean;
import com.axonivy.solutions.process.analyser.utils.FacesContexts;

@FacesConverter("processStartConverter")
public class ProcessStartConverter implements Converter {
  
  private static final String KEY_SEPARATOR = ":::";

  // Pattern contains: Module:::Process:::Start
  private static final String PROCESS_ID_PATTERN = "%s:::%s:::%s";

  // Pattern contains: Module:::Process
  private static final String PROCESS_ID_PATTERN_WITHOUT_START_ELEMENT = "%s:::%s";

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      String[] data = value.split(KEY_SEPARATOR);
      var masterDataBean = FacesContexts.evaluateValueExpression("#{masterDataBean}", MasterDataBean.class);

      List<Process> processElements = masterDataBean.getAvailableProcesses(data[0]);
      var foundProcess =
          processElements.stream().filter(element -> element.getId().equals(data[1])).findAny().orElse(null);

      StartElement foundStartElement = null;
      if (!isMergeProcessStarts() && foundProcess != null) {
        foundStartElement = foundProcess.getStartElements().stream().filter(start -> start.getPid().equals(data[2]))
            .findFirst().orElse(null);
      }
      var processAnalyser = new ProcessAnalyser();
      processAnalyser.setProcess(foundProcess);
      processAnalyser.setStartElement(foundStartElement);
      return processAnalyser;
    } catch (IllegalArgumentException e) {
      throw new ConverterException("Invalid ProcessStart: " + value, e);
    }
  }

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
    if (value == null) {
      return "";
    }
    if (value instanceof ProcessAnalyser) {
      var processAnalyser = ((ProcessAnalyser) value);
      var process = processAnalyser.getProcess();

      if (process != null) {
        if (isMergeProcessStarts()) {
          return PROCESS_ID_PATTERN_WITHOUT_START_ELEMENT.formatted(process.getPmvName(), process.getId());
        } else {
          String startPid = Optional.ofNullable(processAnalyser.getStartElement()).map(StartElement::getPid).orElse("");
          return PROCESS_ID_PATTERN.formatted(process.getPmvName(), process.getId(), startPid);
        }
      }
    }
    throw new ConverterException("Unexpected value type: " + value.getClass().getName());
  }

  private Boolean isMergeProcessStarts() {
    var processesAnalyticsBean =
        FacesContexts.evaluateValueExpression("#{processesAnalyticsBean}", ProcessesAnalyticsBean.class);
    return processesAnalyticsBean.isMergeProcessStarts();
  }
}
