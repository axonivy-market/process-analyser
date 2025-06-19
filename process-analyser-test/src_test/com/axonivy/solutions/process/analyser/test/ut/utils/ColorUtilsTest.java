package com.axonivy.solutions.process.analyser.test.ut.utils;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.DARK_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.LIGHT_TEXT_COLOR;
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
    List<String> gradient = ColorUtils.generateGradientFromRgb("rgb(20, 40, 60)", 5);
    assertThat(gradient).hasSize(5);
    assertThat(gradient.get(0)).startsWith("rgb(");
    assertThat(gradient.get(0)).isNotEqualTo(gradient.get(4));
  }

  @Test
  void test_generateGradientFromRgb_withLightColor() {
    List<String> gradient = ColorUtils.generateGradientFromRgb("rgb(250, 250, 250)", 4);
    assertThat(gradient).hasSize(4);
    assertThat(gradient.get(0)).isEqualTo("rgb(250, 250, 250)");
    assertThat(gradient.get(3)).isNotEqualTo("rgb(250, 250, 250)");
  }

  @Test
  void test_generateGradientFromRgb_invalidFormat_throwsException() {
    String invalidInput = "rgba(255, 255, 255)";
    assertThatThrownBy(() -> ColorUtils.generateGradientFromRgb(invalidInput, 5))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid RGB format");

  }

  @Test
  void testCalculateColorFromList() {
    List<String> colors = List.of(LIGHT_TEXT_COLOR, DARK_TEXT_COLOR);
    assertEquals(LIGHT_TEXT_COLOR, ColorUtils.calculateColorFromList(0.27, colors));
  }

  @Test
  void test_generateGradientFromRgb_withSelectedDarkenColor() {
    String input = "rgb(10, 20, 30)";
    int steps = 5;
    List<String> gradient = ColorUtils.generateGradientFromRgb(input, steps);
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
    String input = "rgb(240, 240, 240)";
    int steps = 6;
    List<String> gradient = ColorUtils.generateGradientFromRgb(input, steps);
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
    String input = "rgb(128, 128, 128)";
    int steps = 4;
    List<String> gradient = ColorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient.get(steps - 1)).isEqualTo(input);
    assertThat(extractBrightness(gradient.get(0))).isGreaterThan(extractBrightness(gradient.get(steps - 1)));
  }

  @Test
  void test_generateGradientFromRgb_withPureBlack() {
    String input = "rgb(0, 0, 0)";
    int steps = 3;
    List<String> gradient = ColorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient.get(0)).isEqualTo("rgb(217, 217, 217)");
    assertThat(gradient.get(steps - 1)).isEqualTo("rgb(0, 0, 0)");
  }

  @Test
  void testGetAccessibleTextColorWithError() {
    String color = "invalid";
    assertThatThrownBy(() -> ColorUtils.getAccessibleTextColor(color)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("#getAccessibleTextColor: Unsupported color: " + color);
  }

  @Test
  void test_generateGradientFromRgb_withPureWhite() {
    String input = "rgb(255, 255, 255)";
    int steps = 3;
    List<String> gradient = ColorUtils.generateGradientFromRgb(input, steps);
    assertThat(gradient.get(0)).isEqualTo("rgb(255, 255, 255)");
    assertThat(gradient.get(steps - 1)).startsWith("rgb(3");
  }
}
