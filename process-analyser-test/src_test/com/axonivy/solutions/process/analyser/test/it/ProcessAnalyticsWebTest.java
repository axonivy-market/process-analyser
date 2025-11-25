package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

@IvyWebTest
public class ProcessAnalyticsWebTest extends WebBaseSetup {

  private static final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:show-statistic-btn";
  private static final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:moduleDropdown";
  private static final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:process-dropdown";
  private static final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:kpiDropdown";
  private static final String CASCADE_DROPDOWN_LIST_SUFFIX = "_panel";
  private static final String CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = " .ui-cascadeselect-label";
  private static final String DISABLE_PROPERTY = "disabled";
  private static final String CHECK_PROPERTY = "checked";
  private static final String PROCESS_FILE = "TestProcess";
  private static final String PROCESS_NAME_EN = "Test process";
  private static final String PROCESS_NAME_DE = "Testprozess";
  private static final String FREQUENCY_OPTION_NAME = "Frequency";
  private static final String DROPDOWN_LABEL_SUFFIX = "_label";
  private static final String TEST_MODULE_NAME = "process-analyser-test";
  private static final String DROPDOWN_LIST_SUFFIX = "_items";
  private static final String MERGE_PROCESS_STARTS_INPUT_SELECTOR = "[id$=':additional-feature:merge-process-starts_input']";
  private static final String MERGE_PROCESS_STARTS_SELECTOR = "[id$=':additional-feature:merge-process-starts']";
  private static final String STANDARD_FILTER_PANEL_GROUP = "process-analytics-form:standard-filter-panel-group:";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() throws InterruptedException {
    login();
    resetLocale();
    startAnalyzingProcess();
    verifyMergeProcessStartToggle();
    turnOffProcessStart();
    verifyMergeProcessStartToggleEmpty();
    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    // Choose test project PM
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    Selenide.sleep(2000);
    // Verify English process name is rendered
    verifyAndSelectAProcess(PROCESS_NAME_EN);
    verifyAndClickItemLabelInDropdown(KPI_DROPDOWN_CSS_SELECTOR, FREQUENCY_OPTION_NAME, CASCADE_DROPDOWN_LIST_SUFFIX,
        CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX);
    // Check the status of show statistic button after data fulfilled
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, StringUtils.EMPTY));

    // Change locale
    changeLocaleToGerman();

    startAnalyzingProcess();
    turnOffProcessStart();
    verifyMergeProcessStartToggleEmpty();
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    Selenide.sleep(2000);
    // Verify German process name is rendered
    verifyAndSelectAProcess(PROCESS_NAME_DE);
    resetLocale();
  }

  @Test
  void showPmvWhenSelectProcess() {
    login();
    startAnalyzingProcess();
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    $(By.id(STANDARD_FILTER_PANEL_GROUP + "moduleDropdown")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "moduleDropdown_1")).click();
    Selenide.sleep(2000);
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmvDropdown")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmvDropdown_1")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmvDropdown")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmvDropdown_2")).click();
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

  private void verifyMergeProcessStartToggle() {
    var toggle = $(MERGE_PROCESS_STARTS_INPUT_SELECTOR);
    toggle.shouldBe(attribute(CHECK_PROPERTY, "true"));
  }

  private void turnOffProcessStart() throws InterruptedException {
    var toggle = $(MERGE_PROCESS_STARTS_SELECTOR);
    toggle.click();
  }

  private void verifyMergeProcessStartToggleEmpty() {
    var toggle = $(MERGE_PROCESS_STARTS_INPUT_SELECTOR);
    toggle.shouldBe(attribute(CHECK_PROPERTY, StringUtils.EMPTY));
    $(PROCESS_DROPDOWN_CSS_SELECTOR).shouldBe(visible, DEFAULT_DURATION);
  }
}
