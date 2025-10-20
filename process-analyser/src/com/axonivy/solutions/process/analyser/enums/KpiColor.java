package com.axonivy.solutions.process.analyser.enums;

import java.util.List;

import com.axonivy.solutions.process.analyser.constants.AnalyserConstants;
import com.axonivy.solutions.process.analyser.utils.ColorUtils;

public enum KpiColor implements HasCmsName {
  FREQUENCY("rgb(70, 180, 100)"), DURATION("rgb(255, 206, 123)");

  private final String baseColor;
  private final List<String> colors;

  private KpiColor(String baseColor) {
    this.baseColor = baseColor;
    this.colors = ColorUtils.generateGradientFromRgb(baseColor, AnalyserConstants.GRADIENT_COLOR_LEVELS);
  }

  public String getBaseColor() {
    return baseColor;
  }

  public List<String> getColors() {
    return colors;
  }

  public static List<String> fromKpiType(KpiType kpiType) {
    return switch (kpiType) {
      case FREQUENCY -> FREQUENCY.colors;
      default -> DURATION.colors;
    };
  }
}
