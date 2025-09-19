package com.axonivy.solutions.process.analyser.core.util;

import static com.axonivy.solutions.process.analyser.core.enums.ElementType.ALTERNATIVE;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.CALL_SUB_END;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.CALL_SUB_START;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.ELEMENT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.EMBEDDED_END;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.EMBEDDED_PROCESS_ELEMENT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.EMBEDDED_START;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.REQUEST_START;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.REST_CLIENT_CALL;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.SCRIPT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.SCRIPT_BPMN_ELEMENT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.SERVICE_BPMN_ELEMENT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.SIGNAL_START_EVENT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.SUB_PROCESS_CALL;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.TASK_END;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.TASK_SWITCH_EVENT;
import static com.axonivy.solutions.process.analyser.core.enums.ElementType.TASK_SWITCH_GATEWAY;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.bo.ElementDisplayName;
import com.axonivy.solutions.process.analyser.core.enums.ElementType;
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
      case RequestStart requestStart -> REQUEST_START;
      case SignalStartEvent signalStart -> SIGNAL_START_EVENT;
      case TaskEnd taskEnd -> TASK_END;
      case Alternative alternative -> ALTERNATIVE;
      case Script script -> SCRIPT;
      case ScriptBpmnElement script -> SCRIPT_BPMN_ELEMENT;
      case ServiceBpmnElement service -> SERVICE_BPMN_ELEMENT;
      case EmbeddedProcessElement embeddedProcess -> EMBEDDED_PROCESS_ELEMENT;
      case EmbeddedStart embedddedStart -> EMBEDDED_START;
      case EmbeddedEnd embedddedEnd -> EMBEDDED_END;
      case CallSubStart callSubStart -> CALL_SUB_START;
      case CallSubEnd callSubEnd -> CALL_SUB_END;
      case SubProcessCall subProcessCall -> SUB_PROCESS_CALL;
      case TaskSwitchEvent taskSwitchEvent -> TASK_SWITCH_EVENT;
      case TaskSwitchGateway taskSwitchGateway -> TASK_SWITCH_GATEWAY;
      case RestClientCall restClientCall -> REST_CLIENT_CALL;
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
