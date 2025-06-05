package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

@IvyWebTest(headless = false)
public class ProcessAnalyticsWebTest extends WebBaseSetup {

  private final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:show-statistic-btn";
  private final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:moduleDropdown";
  private final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:processDropdown";
  private final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:kpiDropdown";
  private final String CASCADE_DROPDOWN_LIST_SUFFIX = "_panel";
  private final String CASCADE_DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX = CASCADE_DROPDOWN_LIST_SUFFIX + " li";
  private final String CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = " .ui-cascadeselect-label";
  private final String DISABLE_PROPERTY = "disabled";
  private final String PROCESS_NAME_EN = "Test process";
  private final String PROCESS_NAME_DE = "Testprozess";
  private final String TEST_MODULE_NAME = "process-analyser-test";
  private final String DROPDOWN_LIST_SUFFIX = "_items";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
    login();
    resetLocale();
    startAnalyzingProcess();
    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    // Choose test project PM
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX, "_label");
    // Verify EN process name is rendered
    verifyAndClickItemLabelInDropdown(PROCESS_DROPDOWN_CSS_SELECTOR, PROCESS_NAME_EN, DROPDOWN_LIST_SUFFIX, "_label");
    verifyAndClickItemLabelInDropdown(KPI_DROPDOWN_CSS_SELECTOR, "Frequency", CASCADE_DROPDOWN_LIST_SUFFIX,
        CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX);
//
//
//    clickFirstOptionFromTheCascadeDropdown(KPI_DROPDOWN_CSS_SELECTOR);
//    // Check the status of show statistic button after data fulfilled
//    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, StringUtils.EMPTY));
//
//    // Change locale
//    changeLocaleToGerman();
//
//    startAnalyzingProcess();
//    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX);
//    // Verify D process name is rendered
//    verifyAndClickItemLabelInDropdown(PROCESS_DROPDOWN_CSS_SELECTOR, PROCESS_NAME_DE, DROPDOWN_LIST_SUFFIX);
  }

  private void clickFirstOptionFromTheCascadeDropdown(String cascadeDropdownCssSelector) {
    // Click target drop down when it's ready
    var dropdown = $(cascadeDropdownCssSelector);
    dropdown.shouldBe(visible, Duration.ofSeconds(2));
    dropdown.click();
    $(cascadeDropdownCssSelector + CASCADE_DROPDOWN_LIST_SUFFIX).shouldBe(visible, Duration.ofSeconds(2));

    SelenideElement targetElement = $$(cascadeDropdownCssSelector + CASCADE_DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX).get(0);
    String selectedOptionLabel = targetElement.text();
    targetElement.click();

    // Check if the label have been change to target option label
    $(cascadeDropdownCssSelector + CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX)
        .shouldHave(Condition.text(selectedOptionLabel), Duration.ofSeconds(1));
  }
}
