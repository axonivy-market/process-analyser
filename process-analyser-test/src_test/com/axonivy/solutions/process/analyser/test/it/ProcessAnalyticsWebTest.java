package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Selenide.$;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;

@IvyWebTest
public class ProcessAnalyticsWebTest extends WebBaseSetup {

  private final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:show-statistic-btn";
  private final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:moduleDropdown";
  private final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:processDropdown";
  private final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:kpiDropdown";
  private final String CASCADE_DROPDOWN_LIST_SUFFIX = "_panel";
  private final String CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = " .ui-cascadeselect-label";
  private final String DISABLE_PROPERTY = "disabled";
  private final String PROCESS_NAME_EN = "Test process";
//  private final String PROCESS_NAME_DE = "Testprozess";
  private final String FREQUENCY_OPTION_NAME = "Frequency";
  private final String DROPDOWN_LABEL_SUFFIX = "_label";

  private final String TEST_MODULE_NAME = "process-analyser-test";
  private final String DROPDOWN_LIST_SUFFIX = "_items";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
//    login();
//    resetLocale();
    startAnalyzingProcess();
    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    // Choose test project PM
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    // Verify English process name is rendered
    verifyAndClickItemLabelInDropdown(PROCESS_DROPDOWN_CSS_SELECTOR, PROCESS_NAME_EN, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    verifyAndClickItemLabelInDropdown(KPI_DROPDOWN_CSS_SELECTOR, FREQUENCY_OPTION_NAME, CASCADE_DROPDOWN_LIST_SUFFIX,
        CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX);
    // Check the status of show statistic button after data fulfilled
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, StringUtils.EMPTY));
//
//    // Change locale
//    changeLocaleToGerman();
//
//    startAnalyzingProcess();
//    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
//        DROPDOWN_LABEL_SUFFIX);
//    // Verify German process name is rendered
//    verifyAndClickItemLabelInDropdown(PROCESS_DROPDOWN_CSS_SELECTOR, PROCESS_NAME_DE, DROPDOWN_LIST_SUFFIX,
//        DROPDOWN_LABEL_SUFFIX);
//    resetLocale();
  }
}
