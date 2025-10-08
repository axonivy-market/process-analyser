package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.codeborne.selenide.SelenideElement;

@IvyWebTest
public class ProcessAnalyticsWebTest extends WebBaseSetup {

  private static final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:show-statistic-btn";
  private static final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:moduleDropdown";
  private static final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:process-analyser-dropdown";
  private static final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:kpiDropdown";
  private static final String CASCADE_DROPDOWN_LIST_SUFFIX = "_panel";
  private static final String CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = " .ui-cascadeselect-label";
  private static final String DISABLE_PROPERTY = "disabled";
  private static final String PROCESS_FILE = "TestProcess";
  private static final String PROCESS_NAME_EN = "Test process";
  private static final String PROCESS_NAME_DE = "Testprozess";
  private static final String FREQUENCY_OPTION_NAME = "Frequency";
  private static final String DROPDOWN_LABEL_SUFFIX = "_label";
  private static final String TEST_MODULE_NAME = "process-analyser-test";
  private static final String DROPDOWN_LIST_SUFFIX = "_items";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
    login();
    resetLocale();
    startAnalyzingProcess();
    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    // Choose test project PM
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    // Verify English process name is rendered
    verifyAndSelectAProcess(PROCESS_NAME_EN);
    verifyAndClickItemLabelInDropdown(KPI_DROPDOWN_CSS_SELECTOR, FREQUENCY_OPTION_NAME, CASCADE_DROPDOWN_LIST_SUFFIX,
        CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX);
    // Check the status of show statistic button after data fulfilled
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, StringUtils.EMPTY));

    // Change locale
    changeLocaleToGerman();

    startAnalyzingProcess();
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    // Verify German process name is rendered
    verifyAndSelectAProcess(PROCESS_NAME_DE);
    resetLocale();
  }

  private void verifyAndSelectAProcess(String startElementName) {
    // Verify and click the process drop-down
    var processDropdown = $(PROCESS_DROPDOWN_CSS_SELECTOR).shouldBe(visible, DEFAULT_DURATION);
    processDropdown.click();

    // Verify selection panel
    var selectionPanel = $(PROCESS_DROPDOWN_CSS_SELECTOR + CASCADE_DROPDOWN_LIST_SUFFIX).shouldBe(visible, DEFAULT_DURATION);

    // Find 1st option (index = 1 to avoid choosing default initial option of null)
    var processFileAndStartName = PROCESS_FILE.concat("/").concat("Start: ").concat(startElementName);
    SelenideElement targetElement = selectionPanel.$$(" li").stream()
        .filter(item -> processFileAndStartName.equals(item.text())).findAny()
        .orElseThrow(() -> new AssertionError(getDropdownItemNotFoundMessage(processFileAndStartName)))
        .shouldBe(visible, DEFAULT_DURATION);
    targetElement.click();
  }
}
