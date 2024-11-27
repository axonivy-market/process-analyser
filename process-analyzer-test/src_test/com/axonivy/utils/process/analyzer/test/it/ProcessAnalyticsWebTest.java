package com.axonivy.utils.process.analyzer.test.it;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Selenide;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static com.codeborne.selenide.Condition.attribute;

import org.junit.jupiter.api.Test;

@IvyWebTest
public class ProcessAnalyticsWebTest {
  private final String TEST_PROCESS_PATH = "/process-analyzer-test/193485C5ABDFEA93/test.ivp";
  private final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:show-statistic-btn";
  private final String SHOW_MODULE_DROPDOWN_BTN_CSS_SELECTOR = "#process-analytics-form\\:moduleDropdown .ui-selectonemenu-trigger";
  private final String SHOW_PROCESS_DROPDOWN_BTN_CSS_SELECTOR ="#process-analytics-form\\:processDropdown .ui-selectonemenu-trigger";
  private final String SHOW_KPI_DROPDOWN_BTN_CSS_SELECTOR ="#process-analytics-form\\:kpiDropdown .ui-selectonemenu-trigger";
  private final String UI_DROPDOWN_MENU_LIST_CSS_SELECTOR = ".ui-selectonemenu-items li";
  
  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
    // Open process analyzer view
    open(EngineUrl.createProcessUrl(TEST_PROCESS_PATH));

    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute("disabled", "true"));

    // Chose module, process, kpi
    $(SHOW_MODULE_DROPDOWN_BTN_CSS_SELECTOR).click();
    $$(UI_DROPDOWN_MENU_LIST_CSS_SELECTOR).get(1).click();
    Selenide.sleep(200);
    $(SHOW_PROCESS_DROPDOWN_BTN_CSS_SELECTOR).click();
    $$(UI_DROPDOWN_MENU_LIST_CSS_SELECTOR).get(1).click();
    Selenide.sleep(200);
    $(SHOW_KPI_DROPDOWN_BTN_CSS_SELECTOR).click();
    $$(UI_DROPDOWN_MENU_LIST_CSS_SELECTOR).get(1).click();
    Selenide.sleep(200);

    //Check the status of show statistic btn after data fulfilled
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute("disabled", ""));
  }

}
