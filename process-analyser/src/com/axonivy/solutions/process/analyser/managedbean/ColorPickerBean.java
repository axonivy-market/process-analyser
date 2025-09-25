package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.COLOR_SEGMENT_ATTRIBUTE;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.GRADIENT_COLOR_LEVELS;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.HYPHEN_REGEX;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.HYPHEN_SIGN;
import static com.axonivy.solutions.process.analyser.core.constants.UserProperty.DURATION_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.UserProperty.DURATION_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.UserProperty.FREQUENCY_COLOR;
import static com.axonivy.solutions.process.analyser.core.constants.UserProperty.FREQUENCY_TEXT_COLOR;
import static com.axonivy.solutions.process.analyser.enums.KpiType.FREQUENCY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;

import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.utils.ColorUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

@ManagedBean
@ViewScoped
public class ColorPickerBean implements Serializable {

  private static final long serialVersionUID = -4814493647451230728L;
  private KpiType selectedKpiType;
  private List<String> colorSegments;
  private List<String> textColors;
  private String selectedColor;
  private int selectedIndex = -1;

  public void initBean(KpiType selectedKpiType, String selectedMap) {
    this.selectedKpiType = selectedKpiType;
    this.colorSegments = new ArrayList<>();
    this.textColors = new ArrayList<>();
    if (selectedKpiType != null) {
      resetSelection();
      Ivy.log().info("colorpicker bean "+selectedMap);
      if ("Heatmap".equalsIgnoreCase(selectedMap)) {
        this.colorSegments = ColorUtils.generateHeatmapColors(GRADIENT_COLOR_LEVELS);
        this.textColors = ColorUtils.getAccessibleTextColors(colorSegments);
        Ivy.log().info(textColors);
//        updateColorProperties();
      } else {
        Ivy.log().info("size"+colorSegments.size());
        getBackgroundAndTextColors();
        Ivy.log().info("size"+colorSegments.getLast());
      }
    }
  }
  public void onSegmentClick(ActionEvent event) {
    selectedIndex = (Integer) event.getComponent().getAttributes().get(COLOR_SEGMENT_ATTRIBUTE);
    selectedColor = colorSegments.get(selectedIndex);
  }

  public void onColorChange() {
    colorSegments = ColorUtils.generateGradientFromRgb(selectedColor, GRADIENT_COLOR_LEVELS);
    textColors = ColorUtils.getAccessibleTextColors(colorSegments);
    updateColorProperties();
  }

  public String getCalulatedCellColor(Double value) {
    return ColorUtils.calculateColorFromList(value, colorSegments);
  }

  public String getAccessibleTextColor(Double value) {
    return ColorUtils.getAccessibleTextColor(getCalulatedCellColor(value));
  }

  private void updateColorProperties() {
    IUser user = Ivy.session().getSessionUser();
    String colorKey = getColorPropertyKey();
    String textKey = getTextColorPropertyKey();

    user.setProperty(colorKey, String.join(HYPHEN_SIGN, colorSegments));
    user.setProperty(textKey, String.join(HYPHEN_SIGN, textColors));
    Ivy.log().fatal(user.getProperty(colorKey) + " " + user.getProperty(textKey));
  }

  public void getBackgroundAndTextColors() {
    IUser user = Ivy.session().getSessionUser();
    String colorKey = getColorPropertyKey();
    String textKey = getTextColorPropertyKey();

    String colorProperty = user.getProperty(colorKey);
    String textProperty = user.getProperty(textKey);

    if (colorProperty != null && textProperty != null) {
      colorSegments = Arrays.asList(colorProperty.split(HYPHEN_REGEX));
      textColors = Arrays.asList(textProperty.split(HYPHEN_REGEX));
    } else {
      colorSegments = ColorUtils.generateColorSegments(selectedKpiType);
      textColors = ColorUtils.getAccessibleTextColors(colorSegments);
    }
  }

  public void resetSelection() {
    selectedIndex = -1;
    selectedColor = null;
    
  }
  public boolean isRenderedColorPicker() {
    return selectedIndex >= 0;
  }

  private String getColorPropertyKey() {
    return FREQUENCY == selectedKpiType ? FREQUENCY_COLOR : DURATION_COLOR;
  }

  private String getTextColorPropertyKey() {
    return FREQUENCY == selectedKpiType ? FREQUENCY_TEXT_COLOR : DURATION_TEXT_COLOR;
  }

  public KpiType getSelectedKpiType() {
    return selectedKpiType;
  }

  public void setSelectedKpiType(KpiType selectedKpiType) {
    this.selectedKpiType = selectedKpiType;
  }

  public List<String> getColorSegments() {
    return colorSegments;
  }

  public void setColorSegments(List<String> colorSegments) {
    this.colorSegments = colorSegments;
  }

  public List<String> getTextColors() {
    return textColors;
  }

  public void setTextColors(List<String> textColors) {
    this.textColors = textColors;
  }

  public String getSelectedColor() {
    return selectedColor;
  }

  public void setSelectedColor(String selectedColor) {
    this.selectedColor = selectedColor;
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public void setSelectedIndex(int selectedIndex) {
    this.selectedIndex = selectedIndex;
  }
}
