package com.axonivy.solutions.process.analyser.managedbean;

import static com.axonivy.solutions.process.analyser.constants.ProcessAnalyticsConstants.COLOR_SEGMENT_ATTRIBUTE;
import static com.axonivy.solutions.process.analyser.constants.ProcessAnalyticsConstants.GRADIENT_COLOR_LEVELS;
import static com.axonivy.solutions.process.analyser.constants.ProcessAnalyticsConstants.HYPHEN_REGEX;
import static com.axonivy.solutions.process.analyser.constants.ProcessAnalyticsConstants.PROCESS_ANALYTIC_PERSISTED_CONFIG;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyserConstants.HYPHEN_SIGN;
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
import com.axonivy.solutions.process.analyser.utils.JacksonUtils;

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
  private ProcessViewerConfig processViewerConfig;

  public void initBean(KpiType selectedKpiType, ColorMode selectedColorMode, ProcessViewerConfig processViewerConfig) {
    this.selectedKpiType = selectedKpiType;
    this.processViewerConfig = processViewerConfig;
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
    if (FREQUENCY == selectedKpiType) {
      processViewerConfig.setFrequencyColor(colorValue);
      processViewerConfig.setFrequencyTextColor(textValue);
    } else {
      processViewerConfig.setDurationColor(colorValue);
      processViewerConfig.setDurationTextColor(textValue);
    }
    user.setProperty(PROCESS_ANALYTIC_PERSISTED_CONFIG, JacksonUtils.convertObjectToJSONString(processViewerConfig));
  }

  public void onChooseColorChooserMode() {
    String colorProperty = FREQUENCY == selectedKpiType ? processViewerConfig.getFrequencyColor()
        : processViewerConfig.getDurationColor();
    String textProperty = FREQUENCY == selectedKpiType ? processViewerConfig.getFrequencyTextColor()
        : processViewerConfig.getDurationTextColor();

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
