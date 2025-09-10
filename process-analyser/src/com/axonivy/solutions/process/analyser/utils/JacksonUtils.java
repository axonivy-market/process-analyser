package com.axonivy.solutions.process.analyser.utils;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;

public class JacksonUtils {
  private static ObjectMapper objectMapper;

  public static String convertObjectToJSONString(Object object) {
    String result = StringUtils.EMPTY;
    try {
      result = getObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      Ivy.log().error(e);
    }
    return result;
  }

  public static <T> T convertStringToObject(String json, Class<T> classType) throws Exception {
    return getObjectMapper().readValue(json, classType);
  }

  private static ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    return objectMapper;
  }
}
