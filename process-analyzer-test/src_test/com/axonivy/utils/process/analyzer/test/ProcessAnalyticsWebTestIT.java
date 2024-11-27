package com.axonivy.utils.process.analyzer.test;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Selenide;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import static com.codeborne.selenide.Condition.attribute;

import org.junit.jupiter.api.Test;

@IvyWebTest
public class ProcessAnalyticsWebTestIT {
  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfiled() {
    // Open process analyzer view
    open(EngineUrl.createProcessUrl("process-analyzer-test/193485C5ABDFEA93/test.ivp"));

    // Check the current status of show statistic button
    $("#process-analytics-form\\:show-statistic-btn").shouldBe(attribute("disabled", "true"));

    // Chose module, process, kpi
    $("#process-analytics-form\\:moduleDropdown .ui-selectonemenu-trigger").click();
    $$(".ui-selectonemenu-items li").get(1).click();
    Selenide.sleep(200);
    $("#process-analytics-form\\:processDropdown .ui-selectonemenu-trigger").click();
    $$(".ui-selectonemenu-items li").get(1).click();
    Selenide.sleep(200);
    $("#process-analytics-form\\:kpiDropdown .ui-selectonemenu-trigger").click();
    $$(".ui-selectonemenu-items li").get(1).click();
    Selenide.sleep(200);

    //Check the status of show statistic btn after data fulfilled
    $("#process-analytics-form\\:show-statistic-btn").shouldBe(attribute("disabled", ""));
  }

}
