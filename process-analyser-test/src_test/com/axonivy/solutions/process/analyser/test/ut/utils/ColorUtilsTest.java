package com.axonivy.solutions.process.analyser.test.ut.utils;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.DARK_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.LIGHT_TEXT_COLOR;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.utils.ColorUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class ColorUtilsTest {

  private static final String RGB_LIGHT_COLOR = "rgb(247, 246, 245)";

  @Test
  void testCalculateColorFromList() {
    List<String> colors = List.of(LIGHT_TEXT_COLOR, DARK_TEXT_COLOR);
    assertEquals(LIGHT_TEXT_COLOR, ColorUtils.calculateColorFromList(0.27, colors));
  }

  @Test
  void testGetAccessibleTextForHexColor() {
    assertEquals(DARK_TEXT_COLOR, ColorUtils.getAccessibleTextColor(LIGHT_TEXT_COLOR));
  }

  @Test
  void testGetAccessibleTextForRGBColor() {
    assertEquals(DARK_TEXT_COLOR, ColorUtils.getAccessibleTextColor(RGB_LIGHT_COLOR));
  }

  @Test
  void testGetAccessibleTextColorWithError() {
    String color = "invalid";
    assertThatThrownBy(() -> ColorUtils.getAccessibleTextColor(color))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("#getAccessibleTextColor: Unsupported color: " + color);
  }

}
