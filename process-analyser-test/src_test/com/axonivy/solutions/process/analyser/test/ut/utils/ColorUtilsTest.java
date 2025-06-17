package com.axonivy.solutions.process.analyser.test.ut.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.utils.ColorUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class ColorUtilsTest {

  @Test
  void testCalculateColorFromList() {
    List<String> colors = List.of("#FFFFFF", "#000000");
    assertEquals("#FFFFFF", ColorUtils.calculateColorFromList(0.27, colors));
  }

  @Test
  void testGetAccessibleTextColor() {
    assertEquals("#000000", ColorUtils.getAccessibleTextColor("#FFFFFF"));
  }

  @Test
  void testGetAccessibleTextColorWithError() {
    String color = "invalid";
    assertThatThrownBy(() -> ColorUtils.getAccessibleTextColor(color))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("#getAccessibleTextColor: Unsupported color: " + color);
  }

}
