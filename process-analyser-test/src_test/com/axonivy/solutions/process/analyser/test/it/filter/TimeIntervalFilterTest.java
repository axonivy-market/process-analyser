package com.axonivy.solutions.process.analyser.test.it.filter;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.solutions.process.analyser.test.it.WebBaseSetup;
import com.axonivy.solutions.process.analyser.utils.DateUtils;
import com.codeborne.selenide.Selenide;

@IvyWebTest
public class TimeIntervalFilterTest extends WebBaseSetup {
  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateUtils.DATE_PATTERN);
  private static SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.DATE_PATTERN);
  private String START_TIME_DATE_PATTERN = "%s 00:00";
  private String END_TIME_DATE_PATTERN = "%s 23:59";

  @BeforeEach
  void startProcess() {
    startAnalyzingProcess();
  }

  @Test
  void checkTimeIntervalDropdownExists() {
    $(By.id("process-analytics-form:standard-filter-panel-group:filter-types")).shouldBe(visible);
  }

  @Test
  void checkDefaultTimeIntervalFilterType() {
    $(By.id("process-analytics-form:standard-filter-panel-group:filter-types_label")).shouldBe(visible).shouldBe(exactText("Today"));
    String today = dateFormat.format(new Date());
    $(By.id("process-analytics-form:standard-filter-panel-group:date-point-selection_input")).shouldBe(visible).shouldHave(value(today));
  }

  @Test
  void testTimeIntervalFilter() {
    // Test Yesterday type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Yesterday']")).shouldBe(visible).click();
    LocalDate yesterday = LocalDate.now().minusDays(1);
    $(By.id("process-analytics-form:standard-filter-panel-group:date-point-selection_input")).shouldBe(visible)
        .shouldHave(value(yesterday.format(dateTimeFormatter)));

    // Test Today type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Today']")).shouldBe(visible).click();
    $(By.id("process-analytics-form:standard-filter-panel-group:date-point-selection_input")).shouldBe(visible)
        .shouldHave(value(dateFormat.format(new Date())));

    // Test Custom type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Custom']")).shouldBe(visible).click();
    String startTimeToday = String.format(START_TIME_DATE_PATTERN, dateFormat.format(new Date()));
    $(By.id("process-analytics-form:custom-date-from_input")).shouldBe(visible).shouldHave(value(startTimeToday));
    String endTimeToday = String.format(END_TIME_DATE_PATTERN, dateFormat.format(new Date()));
    $(By.id("process-analytics-form:custom-date-to_input")).shouldBe(visible).shouldHave(value(endTimeToday));
  }

  private void openFilterTypes() {
    var typeFilter = $(By.id("process-analytics-form:standard-filter-panel-group:filter-types"));
    typeFilter.shouldBe(visible);
    Selenide.sleep(1000);
    typeFilter.click();
    $(By.id("process-analytics-form:standard-filter-panel-group:filter-types_panel")).shouldBe(visible, Duration.ofSeconds(1));
  }
}
