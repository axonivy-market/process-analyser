package com.axonivy.solutions.process.analyser.core.internal;

import static com.axonivy.solutions.process.analyser.core.constants.CoreConstants.SLASH;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.solutions.process.analyser.core.enums.StartElementType;

import ch.ivyteam.ivy.workflow.IProcessStart;
import ch.ivyteam.ivy.workflow.IStartElement;
import ch.ivyteam.ivy.workflow.IStartEventElement;
import ch.ivyteam.ivy.workflow.IWebServiceProcessStartElement;
import ch.ivyteam.ivy.workflow.signal.IStartSignalEventElement;

public class ProcessStartFactory {

  public static com.axonivy.solutions.process.analyser.core.bo.StartElement extractDisplayNameAndType(
      IProcessStart processStart, com.axonivy.solutions.process.analyser.core.bo.StartElement start) {
    return switch (processStart) {
    case IStartElement startElement -> {
      start.setName(getStartName(startElement));
      start.setType(StartElementType.StartElement);
      yield start;
    }
    case IStartEventElement startEventElement -> {
      start.setName(getStartName(startEventElement));
      start.setType(StartElementType.StartEventElement);
      yield start;
    }
    case IStartSignalEventElement startSignalEventElement -> {
      start.setName(getStartName(startSignalEventElement));
      start.setType(StartElementType.StartSignalEventElement);
      yield start;
    }
    case IWebServiceProcessStartElement webServiceProcessStartElement -> {
      start.setName(getStartName(webServiceProcessStartElement));
      start.setType(StartElementType.WebServiceProcessStartElement);
      yield start;
    }
    default -> {
      start.setName(processStart.getName());
      start.setType(StartElementType.StartElement);
      yield start;
    }
    };
  }

  private static String getStartName(IProcessStart startElement) {
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
