package com.axonivy.utils.bpmnstatistic.utils;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;

public class JacksonUtils {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static String convertObjectToJSONString(Object object) {
    String result = StringUtils.EMPTY;
    try {
      result = OBJECT_MAPPER.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      Ivy.log().error(e);
    }
    return result;
  }
}
