package com.axonivy.utils.process.analyser.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NodeType implements HasCmsName {
  @JsonProperty("element")
  ELEMENT,
  @JsonProperty("arrow")
  ARROW;
}