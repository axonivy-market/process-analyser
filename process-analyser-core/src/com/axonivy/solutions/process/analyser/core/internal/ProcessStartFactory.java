package com.axonivy.solutions.process.analyser.core.internal;

import static com.axonivy.solutions.process.analyser.core.constants.CoreConstants.SLASH;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.solutions.process.analyser.core.enums.StartElementType;

import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.internal.element.AbstractStartElement;
import ch.ivyteam.ivy.workflow.internal.element.StartElement;
import ch.ivyteam.ivy.workflow.internal.element.StartEventElement;
import ch.ivyteam.ivy.workflow.internal.ws.WebServiceProcessStartElement;
import ch.ivyteam.ivy.workflow.signal.impl.StartSignalEventElement;

@SuppressWarnings("restriction")
public class ProcessStartFactory {

  public static com.axonivy.solutions.process.analyser.core.bo.StartElement extractDisplayNameAndType(
      IProcessStart processStart, com.axonivy.solutions.process.analyser.core.bo.StartElement start) {
    return switch (processStart) {
    case StartElement startElement -> {
      start.setName(getStartName(startElement));
      start.setType(StartElementType.START_ELEMENT);
      yield start;
    }
    case StartEventElement startEventElement -> {
      start.setName(getStartName(startEventElement));
      start.setType(StartElementType.START_EVENT_ELEMENT);
      yield start;
    }
    case StartSignalEventElement startSignalEventElement -> {
      start.setName(getStartName(startSignalEventElement));
      start.setType(StartElementType.START_SIGNAL_EVENT_ELEMENT);
      yield start;
    }
    case WebServiceProcessStartElement webServiceProcessStartElement -> {
      start.setName(getStartName(webServiceProcessStartElement));
      start.setType(StartElementType.WEB_SERVICE_PROCESS_START_ELEMENT);
      yield start;
    }
    default -> {
      start.setName(processStart.getName());
      start.setType(StartElementType.START_ELEMENT);
      yield start;
    }
    };
  }

  private static String getStartName(AbstractStartElement startElement) {
    if (startElement != null) {
      var localeName = startElement.getName();
      if (StringUtils.isNoneBlank(localeName)) {
        return localeName;
      }

      if (Strings.CS.contains(startElement.getRequestPath(), SLASH)) {
        var requestPaths = StringUtils.split(startElement.getRequestPath(), SLASH);
        return requestPaths[requestPaths.length - 1];
      }
    }
    return StringUtils.EMPTY;
  }
}
