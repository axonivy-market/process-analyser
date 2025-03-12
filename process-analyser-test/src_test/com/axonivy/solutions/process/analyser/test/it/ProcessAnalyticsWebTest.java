package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

@IvyWebTest
public class ProcessAnalyticsWebTest extends WebBaseSetup {

  private final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:show-statistic-btn";
  private final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:moduleDropdown";
  private final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:processDropdown";
  private final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:kpiDropdown";
  private final String DROPDOWN_LIST_SUFFIX = "_items";
  private final String CASCADE_DROPDOWN_LIST_SUFFIX = "_panel";
  private final String DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX = DROPDOWN_LIST_SUFFIX + " li";
  private final String CASCADE_DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX = CASCADE_DROPDOWN_LIST_SUFFIX + " li";
  private final String DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = "_label";
  private final String CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = " .ui-cascadeselect-label";
  private final String DISABLE_PROPERTY = "disabled";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
    // Open process analyzer view
    startAnalyzingProcess();

    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    clickFirstOptionFromTheDropdown(MODULE_DROPDOWN_CSS_SELECTOR);
    clickFirstOptionFromTheDropdown(PROCESS_DROPDOWN_CSS_SELECTOR);
    clickFirstOptionFromTheCascadeDropdown(KPI_DROPDOWN_CSS_SELECTOR);

    // Check the status of show statistic button after data fulfilled
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, StringUtils.EMPTY));
  }

  private void clickFirstOptionFromTheDropdown(String dropdownCssSelector) {
    // Click target drop down when it's ready
    var dropdown = $(dropdownCssSelector);
    dropdown.shouldBe(visible);
    Selenide.sleep(1000);
    dropdown.click();
    $(dropdownCssSelector + DROPDOWN_LIST_SUFFIX).shouldBe(visible, Duration.ofSeconds(2));

    // Find 1st option (index = 1 to avoid choosing default initial option of null)
    SelenideElement targetElement = $$(dropdownCssSelector + DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX).get(1);
    String selectedOptionLabel = targetElement.text();
    targetElement.click();

    // Check if the label have been change to target option label
    $(dropdownCssSelector + DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX).shouldHave(Condition.text(selectedOptionLabel),
        Duration.ofSeconds(1));
  }

  private void clickFirstOptionFromTheCascadeDropdown(String cascadeDropdownCssSelector) {
    // Click target drop down when it's ready
    var dropdown = $(cascadeDropdownCssSelector);
    dropdown.shouldBe(visible);
    Selenide.sleep(1000);
    dropdown.click();
    $(cascadeDropdownCssSelector + CASCADE_DROPDOWN_LIST_SUFFIX).shouldBe(visible, Duration.ofSeconds(2));

    SelenideElement targetElement = $$(cascadeDropdownCssSelector + CASCADE_DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX).get(0);
    String selectedOptionLabel = targetElement.text();
    targetElement.click();

    // Check if the label have been change to target option label
    $(cascadeDropdownCssSelector + CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX).shouldHave(Condition.text(selectedOptionLabel),
        Duration.ofSeconds(1));
  }
}
