package com.axonivy.solutions.process.analyser.utils;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.bo.ElementDisplayName;
import com.axonivy.solutions.process.analyser.enums.ElementType;
import static com.axonivy.solutions.process.analyser.enums.ElementType.ALTERNATIVE;
import static com.axonivy.solutions.process.analyser.enums.ElementType.CALL_SUB_END;
import static com.axonivy.solutions.process.analyser.enums.ElementType.CALL_SUB_START;
import static com.axonivy.solutions.process.analyser.enums.ElementType.ELEMENT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.EMBEDDED_END;
import static com.axonivy.solutions.process.analyser.enums.ElementType.EMBEDDED_PROCESS_ELEMENT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.EMBEDDED_START;
import static com.axonivy.solutions.process.analyser.enums.ElementType.REQUEST_START;
import static com.axonivy.solutions.process.analyser.enums.ElementType.REST_CLIENT_CALL;
import static com.axonivy.solutions.process.analyser.enums.ElementType.SCRIPT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.SCRIPT_BPMN_ELEMENT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.SERVICE_BPMN_ELEMENT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.SIGNAL_START_EVENT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.SUB_PROCESS_CALL;
import static com.axonivy.solutions.process.analyser.enums.ElementType.TASK_END;
import static com.axonivy.solutions.process.analyser.enums.ElementType.TASK_SWITCH_EVENT;
import static com.axonivy.solutions.process.analyser.enums.ElementType.TASK_SWITCH_GATEWAY;
import com.axonivy.solutions.process.analyser.internal.ProcessUtils;

import ch.ivyteam.ivy.application.project.Project;
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

public class ProcessElementUtils {

  private static final List<Class<?>> PROCESS_START_CLASSES = List.of(RequestStart.class, ProgramStart.class,
      SignalStartEvent.class, TriggerCall.class);

  private ProcessElementUtils() { }

  public static List<ElementDisplayName> listAllProcessElementAsRawPID(Project pmv, String processId,
      String startElementPID) {
    List<ProcessElement> processElements = ProcessUtils.getProcessElementsFrom(processId, pmv);
    removeAnotherStartElementsBySelectedStartPID(processElements, startElementPID);
    return processElements.stream()
        .map(element -> buildElementDisplayName(element))
        .toList();
  }

  private static ElementDisplayName buildElementDisplayName(ProcessElement processElement) {
    String pid = PIDUtils.getId(processElement.getPid());
    ElementType type = determineElementType(processElement);
    var displayName = StringUtils.defaultIfBlank(processElement.getName(), pid);
    var elementDisplayName = new ElementDisplayName(pid, displayName);
    elementDisplayName.setElementType(type);
    return elementDisplayName;
  }

  private static ElementType determineElementType(ProcessElement processElement) {
    return switch (processElement) {
      case RequestStart _ -> REQUEST_START;
      case SignalStartEvent _ -> SIGNAL_START_EVENT;
      case TaskEnd _ -> TASK_END;
      case Alternative _ -> ALTERNATIVE;
      case Script _ -> SCRIPT;
      case ScriptBpmnElement _ -> SCRIPT_BPMN_ELEMENT;
      case ServiceBpmnElement _ -> SERVICE_BPMN_ELEMENT;
      case EmbeddedProcessElement _ -> EMBEDDED_PROCESS_ELEMENT;
      case EmbeddedStart _ -> EMBEDDED_START;
      case EmbeddedEnd _ -> EMBEDDED_END;
      case CallSubStart _ -> CALL_SUB_START;
      case CallSubEnd _ -> CALL_SUB_END;
      case SubProcessCall _ -> SUB_PROCESS_CALL;
      case TaskSwitchEvent _ -> TASK_SWITCH_EVENT;
      case TaskSwitchGateway _ -> TASK_SWITCH_GATEWAY;
      case RestClientCall _ -> REST_CLIENT_CALL;
      default -> ELEMENT;
    };
  }

  public static void removeAnotherStartElementsBySelectedStartPID(List<ProcessElement> processElements,
      String startElementPID) {
    if (CollectionUtils.isEmpty(processElements)) {
      return;
    }

    List<ProcessElement> remainingStartElementOnProcess = processElements.stream()
        .filter(filterProcessStartElement())
        .filter(element -> !PIDUtils.getId(element.getPid()).equals(startElementPID))
        .toList();
    for (var startElement : remainingStartElementOnProcess) {
      boolean foundStartPoint = processElements.stream()
          .anyMatch(element -> PIDUtils.equalsPID(element.getPid(), startElement.getPid()));
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
