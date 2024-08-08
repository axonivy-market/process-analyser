package com.axonivy.utils.bpmnstatistic.utils;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;

public class JacksonUtils {

  public static String convertObjectToJSONString(Object object) {
    String result = StringUtils.EMPTY;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      result = objectMapper.writeValueAsString(object);
      Ivy.log().info(result);
    } catch (JsonProcessingException e) {
      Ivy.log().error(e);
    }

    return result;
  }
}
