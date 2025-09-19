package com.axonivy.solutions.process.analyser.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NodeType implements HasCmsName {
  @JsonProperty("element")
  ELEMENT,
  @JsonProperty("arrow")
  ARROW;
}