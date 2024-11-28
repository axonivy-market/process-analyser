package com.axonivy.utils.process.analyser.test.it;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import ch.ivyteam.ivy.environment.IvyTest;

import static com.codeborne.selenide.Selenide.open;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Condition.visible;

import static com.codeborne.selenide.Condition.attribute;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

@IvyWebTest
@IvyTest
public class ProcessAnalyticsWebTest {
  private final String TEST_PROCESS_PATH = "/process-analyser-test/193485C5ABDFEA93/test.ivp";
  private final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:show-statistic-btn";
  private final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:moduleDropdown";
  private final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:processDropdown";
  private final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:kpiDropdown";
  private final String DROPDOWN_LIST_SUFFIX = "_items";
  private final String DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX = DROPDOWN_LIST_SUFFIX + " li";
  private final String DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = "_label";
  private final String DISABLE_PROPERTY = "disabled";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
    // Open process analyzer view
    open(EngineUrl.createProcessUrl(TEST_PROCESS_PATH));

    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    clickFirstOptionFromTheDropdown(MODULE_DROPDOWN_CSS_SELECTOR);
    clickFirstOptionFromTheDropdown(PROCESS_DROPDOWN_CSS_SELECTOR);
    clickFirstOptionFromTheDropdown(KPI_DROPDOWN_CSS_SELECTOR);

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
}
