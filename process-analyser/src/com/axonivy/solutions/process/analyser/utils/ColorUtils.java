package com.axonivy.solutions.process.analyser.utils;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.COMMA;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.DARK_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.HASHTAG;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.LIGHT_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.NON_DIGIT_COMMA_REGEX;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.RGB;

import java.util.List;;

public class ColorUtils {

  public static String calculateColorFromList(Double value, List<String> colors) {
    int index = (int) Math.floor(value * colors.size());
    index = Math.min(Math.max(index, 0), colors.size() - 1);
    return colors.get(index);
  }

  public static String getAccessibleTextColor(String color) {
    int r, g, b;
    if (color.startsWith(HASHTAG)) {
      // Stripping the #
      // Parsing the 6-character hex as a single integer
      // Extracting R, G, B by bit-shifting and masking
      int val = Integer.parseInt(color.substring(1), 16);
      r = (val >> 16) & 0xFF;
      g = (val >> 8) & 0xFF;
      b = val & 0xFF;
    } else if (color.startsWith(RGB)) {
      String[] parts = color.replaceAll(NON_DIGIT_COMMA_REGEX, "").split(COMMA);
      r = Integer.parseInt(parts[0]);
      g = Integer.parseInt(parts[1]);
      b = Integer.parseInt(parts[2]);
    } else {
      throw new IllegalArgumentException("#getAccessibleTextColor: Unsupported color: " + color);
    }
    // 0.299 * r + 0.587 * g + 0.114 * b:
    // Luminance formula for determine dark and light color
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.5 ? DARK_TEXT_COLOR : LIGHT_TEXT_COLOR;
  }

}
