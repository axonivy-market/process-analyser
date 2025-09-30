package com.axonivy.solutions.process.analyser.core.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import ch.ivyteam.ivy.process.model.value.PID;

public class PIDUtils {

  private PIDUtils() { }
  
  public static String getId(PID pid) {
    return getId(pid, false);
  }

  public static String getId(PID pid, boolean shouldLookupParent) {
    if (pid == null) {
      return EMPTY;
    }
    if (shouldLookupParent && pid.getParent() != null) {
      return pid.getParent().getRawPid();
    }
    return pid.getRawPid();
  }

  public static boolean equalsPID(PID pidSource, PID pidTarget) {
    return getId(pidSource).contentEquals(getId(pidTarget));
  }
}
