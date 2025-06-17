package com.axonivy.solutions.process.analyser.enums;

import java.util.List;

public enum KpiColor implements HasCmsName {
  FREQUENCY(List.of("#EDFAF1", "#C8F0D4", "#ADE8BF", "#87DEA1", "#70D78F", "#4CCD73", "#45BB69", "#369252", "#2A713F",
      "#205630")),

  DURATION(List.of("#FFF7EA", "#FFEDCD", "#FFDEA5", "#FFCE7B", "#FFC054", "#FFB22E", "#D99727", "#B57E21", "#91651A",
      "#735015"));

  private final List<String> colors;

  KpiColor(List<String> colors) {
    this.colors = colors;
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
