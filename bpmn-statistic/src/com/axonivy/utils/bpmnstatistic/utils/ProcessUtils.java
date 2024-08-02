package com.axonivy.utils.bpmnstatistic.utils;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.bpmnstatistic.constants.ProcessMonitorConstants;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.BaseElement;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;

@SuppressWarnings("restriction")
public class ProcessUtils {

  public static String getProcessRawPidFromElement(String targetElementId) {
    return targetElementId.split(ProcessMonitorConstants.HYPHEN_SIGN)[0];
  }

  public static String getProcessRawPidFromElement(BaseElement targetElement) {
    return getProcessRawPidFromElement(targetElement.getPid().toString());
  }


  public static String getElementRawPid(String elementid) {
    if (StringUtils.isBlank(elementid)) {
      return StringUtils.EMPTY;
    }
    int firstHyphen = elementid.indexOf(ProcessMonitorConstants.HYPHEN_SIGN);
    return elementid.substring(firstHyphen + 1);
  }

  public static String getElementRawPid(BaseElement targetElement) {
    String elementId = targetElement.getPid().toString();
    return getElementRawPid(elementId);
  }

  public static long getCurrentCaseId() {
    return Sudo.get(() -> {
      return Ivy.wf().getCurrentCase().getId();
    });
  }

  public static ITask getCurrentTask() {
    return Sudo.get(() -> {
      return Ivy.wf().getCurrentTask();
    });
  }
}
