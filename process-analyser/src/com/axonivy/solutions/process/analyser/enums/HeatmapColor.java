package com.axonivy.solutions.process.analyser.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing predefined heatmap colors that transition from green to red.
 * This enum provides easy management and customization of heatmap color palettes.
 */
public enum HeatmapColor {
  DARK_GREEN("rgb(0,150,0)"),
  GREEN("rgb(0,200,0)"),
  LIGHT_GREEN("rgb(64,225,0)"),
  BRIGHT_GREEN("rgb(100,255,0)"),
  YELLOW_GREEN("rgb(200,255,0)"),
  YELLOW("rgb(255,255,0)"),
  ORANGE_YELLOW("rgb(255,200,0)"),
  ORANGE("rgb(255,100,0)"),
  RED_ORANGE("rgb(255,64,0)"),
  RED("rgb(200,0,0)"),
  DARK_RED("rgb(175,0,0)");

  private final String rgbValue;

  HeatmapColor(String rgbValue) {
    this.rgbValue = rgbValue;
  }

  public String getRgbValue() {
    return rgbValue;
  }

  public static List<String> getAllColors() {
    return Arrays.stream(values())
        .map(HeatmapColor::getRgbValue)
        .collect(Collectors.toList());
  }
}