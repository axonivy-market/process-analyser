package com.axonivy.solutions.process.analyser.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.util.Strings;

import com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants;
import com.axonivy.solutions.process.analyser.enums.KpiColor;
import com.axonivy.solutions.process.analyser.enums.KpiType;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.COMMA;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.DARK_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.HASHTAG;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.LIGHT_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.NON_DIGIT_COMMA_REGEX;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.RGB_PREFIX;;

public class ColorUtils {
  public static List<String> generateColorSegments(KpiType selectedKpiType) {
    return KpiColor.fromKpiType(selectedKpiType);
  }

  /**
   * Generates a gradient of RGB colors from the selected color. If the color is light, it produces a darkening
   * gradient. If the color is dark, it produces a brightening gradient.
   *
   * @param rgbColor RGB string in the format "rgb(r, g, b)"
   * @param steps Number of gradient steps to generate
   * @return List of RGB color strings forming the gradient
   */
  public static List<String> generateGradientFromRgb(String rgbColor, int steps) {
    Matcher matcher = Pattern.compile(ProcessAnalyticsConstants.RGB_REGEX_PATTERN).matcher(rgbColor);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid RGB format: " + rgbColor);
    }

    int r = Integer.parseInt(matcher.group(1));
    int g = Integer.parseInt(matcher.group(2));
    int b = Integer.parseInt(matcher.group(3));

    double perceivedLuminance = 0.299 * r + 0.587 * g + 0.114 * b;
    boolean isLightColor = perceivedLuminance > 205;

    List<String> gradientColors = new ArrayList<>(steps);

    for (int i = 0; i < steps; i++) {
      float t = (float) i / (steps - 1);
      float adjustmentFactor = isLightColor ? t * 0.85f : (1 - t) * 0.85f;

      int adjustedRed = adjustColor(r, adjustmentFactor, isLightColor);
      int adjustedGreen = adjustColor(g, adjustmentFactor, isLightColor);
      int adjustedBlue = adjustColor(b, adjustmentFactor, isLightColor);

      gradientColors.add(String.format(ProcessAnalyticsConstants.RGB_FORMAT, adjustedRed, adjustedGreen, adjustedBlue));
    }
    return gradientColors;
  }

  private static int adjustColor(int baseValue, float adjustmentFactor, boolean shouldDarken) {
    int value = shouldDarken ? Math.round(baseValue * (1 - adjustmentFactor))
        : Math.round(baseValue + (255 - baseValue) * adjustmentFactor);
    return Math.max(0, Math.min(255, value));
  }

  public static List<String> getAccessibleTextColors(List<String> colors) {
    List<String> textColors = new ArrayList<>();
    for (String color : colors) {
      textColors.add(getAccessibleTextColor(color));
    }
    return textColors;
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
    } else if (color.startsWith(RGB_PREFIX)) {
      String[] parts = color.replaceAll(NON_DIGIT_COMMA_REGEX, Strings.EMPTY).split(COMMA);
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
