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
import com.axonivy.solutions.process.analyser.utils.FacesContexts;

@FacesConverter("processStartConverter")
public class ProcessStartConverter implements Converter {
  
  private static final String KEY_SEPERATOR = ":::";

  // Pattern contains: Module:::Process:::Start
  private static final String PROCESS_ID_PATTERN = "%s:::%s:::%s";

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      String[] data = value.split(KEY_SEPERATOR);
      var masterDataBean = FacesContexts.evaluateValueExpression("#{masterDataBean}", MasterDataBean.class);
      List<Process> processElements = masterDataBean.getAvailableProcesses(data[0]);
      var foundProcess = processElements.stream().filter(element -> element.getId().equals(data[1]))
          .findAny();
      var foundStartElement = foundProcess.stream().map(Process::getStartElements).flatMap(List::stream)
          .filter(start -> start.getPid().equals(data[2])).findAny();
      ProcessAnalyser processAnalyser = new ProcessAnalyser();
      processAnalyser.setProcess(foundProcess.orElse(null));
      processAnalyser.setStartElement(foundStartElement.orElse(null));
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
      if (process == null) {
        return null;
      }
      var startElement = Optional.ofNullable(processAnalyser.getStartElement()).map(StartElement::getPid).orElse("");
      return PROCESS_ID_PATTERN.formatted(process.getPmvName(),
          process.getId(),
          startElement);
    }
    throw new ConverterException("Unexpected value type: " + value.getClass().getName());
  }
}