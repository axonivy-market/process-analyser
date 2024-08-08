package com.axonivy.utils.bpmnstatistic.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class DateUtils {

  public static String formatDate(Date date) {
    if (date == null) {
      return StringUtils.EMPTY;
    }
    DateFormat dateFormat = new SimpleDateFormat(Ivy.cms().co("/Patterns/DatePattern"));
    return dateFormat.format(date);
  }
}
