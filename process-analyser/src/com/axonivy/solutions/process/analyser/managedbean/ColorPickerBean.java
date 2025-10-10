package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.COLOR_SEGMENT_ATTRIBUTE;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.GRADIENT_COLOR_LEVELS;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.HYPHEN_REGEX;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.HYPHEN_SIGN;
import static com.axonivy.solutions.process.analyser.enums.KpiType.FREQUENCY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.bo.ProcessViewerConfig;
import com.axonivy.solutions.process.analyser.enums.ColorMode;
import com.axonivy.solutions.process.analyser.enums.HeatmapColor;
import com.axonivy.solutions.process.analyser.enums.KpiType;
import com.axonivy.solutions.process.analyser.utils.ColorUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IUser;

@ManagedBean
@ViewScoped
public class ColorPickerBean implements Serializable {

  private static final long serialVersionUID = -4814493647451230728L;
  //TODO: convert to constant
  private final String PERSISTED_CONFIG = "PERSISTED_CONFIG";
  private KpiType selectedKpiType;
  private List<String> colorSegments;
  private List<String> textColors;
  private String selectedColor;
  private int selectedIndex = -1;

  public void initBean(KpiType selectedKpiType, ColorMode selectedColorMode) {
    this.selectedKpiType = selectedKpiType;
    this.colorSegments = new ArrayList<>();
    this.textColors = new ArrayList<>();
    if (selectedKpiType != null) {
      resetSelection();
      if (selectedColorMode != null && selectedColorMode.isHeatmap()) {
        onChooseHeatMapMode();
      } else {
        onChooseColorChooserMode();
      }
    }
  }

  public void onChooseHeatMapMode() {
    this.colorSegments = HeatmapColor.getAllColors();
    this.textColors = ColorUtils.getAccessibleTextColors(colorSegments);
  }

  public boolean checkRenderCondition(ColorMode selectedColorMode) {
    return selectedColorMode != null && selectedColorMode.isCustom() && isRenderedColorPicker();
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
    if (CollectionUtils.isEmpty(colorSegments) || CollectionUtils.isEmpty(textColors)) {
      return;
    }
    String colorValue = String.join(HYPHEN_SIGN, colorSegments);
    String textValue = String.join(HYPHEN_SIGN, textColors);
    var persistedConfig = ProcessViewerConfig.fromJson(user.getProperty(PERSISTED_CONFIG));
    if (FREQUENCY == selectedKpiType) {
      persistedConfig.setFrquencyColor(colorValue);
      persistedConfig.setFrquencyTextColor(textValue);
    } else {
      persistedConfig.setDurationColor(colorValue);
      persistedConfig.setDurationTextColor(textValue);
    }
    user.setProperty(PERSISTED_CONFIG, persistedConfig.toJson());
  }

  public void onChooseColorChooserMode() {
    IUser user = Ivy.session().getSessionUser();
    var persistedConfig = ProcessViewerConfig.fromJson(user.getProperty(PERSISTED_CONFIG));

    String colorProperty = FREQUENCY == selectedKpiType ? persistedConfig.getFrquencyColor()
        : persistedConfig.getDurationColor();
    String textProperty = FREQUENCY == selectedKpiType ? persistedConfig.getFrquencyTextColor()
        : persistedConfig.getDurationTextColor();

    if (StringUtils.isNoneBlank(colorProperty, textProperty)) {
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
