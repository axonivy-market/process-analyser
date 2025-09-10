package com.axonivy.solutions.process.analyser.core.util;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.bo.ElementDisplayName;
import com.axonivy.solutions.process.analyser.core.internal.ProcessUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.process.model.element.EmbeddedProcessElement;
import ch.ivyteam.ivy.process.model.element.ProcessElement;
import ch.ivyteam.ivy.process.model.element.activity.RestClientCall;
import ch.ivyteam.ivy.process.model.element.activity.Script;
import ch.ivyteam.ivy.process.model.element.activity.SubProcessCall;
import ch.ivyteam.ivy.process.model.element.activity.TriggerCall;
import ch.ivyteam.ivy.process.model.element.activity.bpmn.ScriptBpmnElement;
import ch.ivyteam.ivy.process.model.element.activity.bpmn.ServiceBpmnElement;
import ch.ivyteam.ivy.process.model.element.event.end.CallSubEnd;
import ch.ivyteam.ivy.process.model.element.event.end.EmbeddedEnd;
import ch.ivyteam.ivy.process.model.element.event.end.TaskEnd;
import ch.ivyteam.ivy.process.model.element.event.intermediate.TaskSwitchEvent;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.element.event.start.EmbeddedStart;
import ch.ivyteam.ivy.process.model.element.event.start.ProgramStart;
import ch.ivyteam.ivy.process.model.element.event.start.RequestStart;
import ch.ivyteam.ivy.process.model.element.event.start.SignalStartEvent;
import ch.ivyteam.ivy.process.model.element.gateway.Alternative;
import ch.ivyteam.ivy.process.model.element.gateway.TaskSwitchGateway;

@SuppressWarnings("restriction")
public class ProcessElementUtils {

  private static final List<Class<?>> PROCESS_START_CLASSES = List.of(RequestStart.class, ProgramStart.class,
      SignalStartEvent.class, TriggerCall.class);

  private ProcessElementUtils() { }

  public static List<ElementDisplayName> listAllProcessElementAsRawPID(IProcessModelVersion pmv, String processId,
      String startElementPID) {
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFrom(processId, pmv);
    removeAnotherStartElementsBySelectedStartPID(processElements, startElementPID);
    return processElements.stream()
        .map(p -> mapToNameOrPID(p))
        .toList();
  }

  private static ElementDisplayName mapToNameOrPID(ProcessElement processElement) {
    String typeLabel = determineElementType(processElement);
    var displayName = StringUtils.isBlank(processElement.getName()) ? PIDUtils.getId(processElement.getPid())
        : typeLabel.concat(processElement.getName());
    var elementDisplayName = new ElementDisplayName();
    elementDisplayName.setDisplayName(displayName);
    elementDisplayName.setPid(PIDUtils.getId(processElement.getPid()));
    return elementDisplayName;
  }

  private static String determineElementType(ProcessElement processElement) {
    return switch (processElement) {
      case RequestStart requestStart -> "Request start: ";
      case SignalStartEvent signalStart -> "Signal start: ";
      case TaskEnd taskEnd -> "Task end: ";
      case Alternative alternative -> "Alternative: ";
      case Script script -> "Script: ";
      case ScriptBpmnElement script -> "Script Bpmn: ";
      case ServiceBpmnElement service -> "Service Bpmn: ";
      case EmbeddedProcessElement embeddedProcess -> "Embedded process: ";
      case EmbeddedStart embedddedStart -> "Embedded start: ";
      case EmbeddedEnd embedddedEnd -> "Embedded end: ";
      case CallSubStart callSubStart -> "Call Sub start: ";
      case CallSubEnd callSubEnd -> "Call Sub end: ";
      case SubProcessCall subProcessCall -> "Sub process call: ";
      case TaskSwitchEvent taskSwitchEvent -> "Task: ";
      case TaskSwitchGateway taskSwitchGateway -> "Task gateway";
      case RestClientCall restClientCall -> "Rest client call: ";
      default -> "Element: ";
    };
  }

  public static void removeAnotherStartElementsBySelectedStartPID(List<ProcessElement> processElements,
      String startElementPID) {
    if (CollectionUtils.isEmpty(processElements)) {
      return;
    }

    List<ProcessElement> anotherStartElements = processElements.stream()
        .filter(filterProcessStartElement())
        .filter(e -> !PIDUtils.getId(e.getPid()).equals(startElementPID))
        .toList();
    for (var startElement : anotherStartElements) {
      boolean foundStartPoint = processElements.stream().anyMatch(
          processElement -> PIDUtils.equalsPID(processElement.getPid(), startElement.getPid()));
      if (foundStartPoint) {
        processElements.remove(startElement);
      }
    }
  }

  public static Predicate<? super ProcessElement> filterProcessStartElement() {
    return processElement -> PROCESS_START_CLASSES.stream()
        .anyMatch(startClass -> startClass.isInstance(processElement));
  }

}
