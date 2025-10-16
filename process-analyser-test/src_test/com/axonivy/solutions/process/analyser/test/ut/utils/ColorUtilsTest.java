package com.axonivy.solutions.process.analyser.test.ut.utils;

import static com.axonivy.solutions.process.analyser.constants.ProcessAnalyticsConstants.DARK_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.constants.ProcessAnalyticsConstants.LIGHT_TEXT_COLOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.enums.KpiColor;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.test.BaseSetup;
import com.axonivy.solutions.process.analyser.utils.ColorUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class ColorUtilsTest extends BaseSetup {
  protected static final String PURE_BLACK = "rgb(0, 0, 0)";
  protected static final String PURE_WHITE = "rgb(255, 255, 255)";
  protected static final String LIGHT_GRAY = "rgb(250, 250, 250)";
  protected static final String NEUTRAL_GRAY = "rgb(217, 217, 217)";
  protected static final String MEDIUM_GRAY = "rgb(128, 128, 128)";
  protected static final String DARK_BLUE_GRAY = "rgb(20, 40, 60)";
  protected static final String EXTREME_DARK_BLUE_GRAY = "rgb(10, 20, 30)";

  private static final String RGB_LIGHT_COLOR = "rgb(247, 246, 245)";

  @Test
  void test_generateColorSegments_withFrequencyKpi() {
    List<String> frequencyColors = KpiColor.FREQUENCY.getColors();
    List<String> colorSegments = ColorUtils.generateColorSegments(KpiType.FREQUENCY);
    assertThat(colorSegments).containsExactlyElementsOf(frequencyColors);
  }

  @Test
  void test_generateColorSegments_withDurationKpi() {
    List<String> durationColors = KpiColor.DURATION.getColors();
    List<String> colorSegments = ColorUtils.generateColorSegments(KpiType.DURATION);
    assertThat(colorSegments).containsExactlyElementsOf(durationColors);
  }

  @Test
  void test_generateGradientFromRgb_withDarkColor() {
    List<String> gradient = ColorUtils.generateGradientFromRgb(DARK_BLUE_GRAY, 5);
    assertThat(gradient).hasSize(5);
    assertThat(gradient.get(0)).isNotEqualTo(gradient.get(4));
  }

  @Test
  void test_generateGradientFromRgb_withLightColor() {
    List<String> gradient = ColorUtils.generateGradientFromRgb(LIGHT_GRAY, 4);
    assertThat(gradient).hasSize(4);
    assertThat(gradient.get(0)).isEqualTo(LIGHT_GRAY);
    assertThat(gradient.get(3)).isNotEqualTo(LIGHT_GRAY);
  }

  @Test
  void testCalculateColorFromList() {
    List<String> colors = List.of(LIGHT_TEXT_COLOR, DARK_TEXT_COLOR);
    assertEquals(LIGHT_TEXT_COLOR, ColorUtils.calculateColorFromList(0.27, colors));
  }

  @Test
  void test_generateGradientFromRgb_withSelectedDarkenColor() {
    int steps = 5;
    List<String> gradient = ColorUtils.generateGradientFromRgb(EXTREME_DARK_BLUE_GRAY, steps);
    assertThat(gradient).hasSize(steps);

    for (int i = 1; i < steps; i++) {
      int prev = extractBrightness(gradient.get(i - 1));
      int curr = extractBrightness(gradient.get(i));
      assertThat(curr).isLessThanOrEqualTo(prev);
    }
  }

  @Test
  void testGetAccessibleTextForHexColor() {
    assertEquals(DARK_TEXT_COLOR, ColorUtils.getAccessibleTextColor(LIGHT_TEXT_COLOR));
  }

  @Test
  void test_generateGradientFromRgb_withSelectedLightenColor() {
    int steps = 6;
    List<String> gradient = ColorUtils.generateGradientFromRgb(LIGHT_GRAY, steps);
    assertThat(gradient).hasSize(steps);
    for (int i = 1; i < steps; i++) {
      int prev = extractBrightness(gradient.get(i - 1));
      int curr = extractBrightness(gradient.get(i));
      assertThat(curr).isLessThanOrEqualTo(prev);
    }
  }

  @Test
  void testGetAccessibleTextForRGBColor() {
    assertEquals(DARK_TEXT_COLOR, ColorUtils.getAccessibleTextColor(RGB_LIGHT_COLOR));
  }

  @Test
  void test_generateGradientFromRgb_middleGray_brightensTowardWhite() {
    int steps = 4;
    List<String> gradient = ColorUtils.generateGradientFromRgb(MEDIUM_GRAY, steps);
    assertThat(gradient.get(steps - 1)).isEqualTo(MEDIUM_GRAY);
    assertThat(extractBrightness(gradient.get(0))).isGreaterThan(extractBrightness(gradient.get(steps - 1)));
  }

  @Test
  void test_generateGradientFromRgb_withPureBlack() {
    int steps = 3;
    List<String> gradient = ColorUtils.generateGradientFromRgb(PURE_BLACK, steps);
    assertThat(gradient.get(0)).isEqualTo(NEUTRAL_GRAY);
    assertThat(gradient.get(steps - 1)).isEqualTo(PURE_BLACK);
  }

  @Test
  void testGetAccessibleTextColorWithError() {
    String color = "invalid";
    assertThatThrownBy(() -> ColorUtils.getAccessibleTextColor(color)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("#getAccessibleTextColor: Unsupported color: " + color);
  }

  @Test
  void test_generateGradientFromRgb_withPureWhite() {
    int steps = 3;
    List<String> gradient = ColorUtils.generateGradientFromRgb(PURE_WHITE, steps);
    assertThat(gradient.get(0)).isEqualTo(PURE_WHITE);
    assertThat(gradient.get(steps - 1)).startsWith("rgb(3");
  }
}
