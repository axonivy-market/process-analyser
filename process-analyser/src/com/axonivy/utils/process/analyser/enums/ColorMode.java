package com.axonivy.utils.process.analyser.enums;

public enum ColorMode implements HasCmsName {
  HEATMAP, CUSTOM;

  public boolean isHeatmap() {
    return this == HEATMAP;
  }

  public boolean isCustom() {
    return this == CUSTOM;
  }
}
