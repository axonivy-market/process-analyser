package com.axonivy.utils.process.analyzer.test.filter;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.utils.process.analyzer.utils.DateUtils;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;

@IvyWebTest(headless = false)
public class TimeIntervalFilterTest {
  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateUtils.DATE_PATTERN);
  private static SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.DATE_PATTERN);
  private String FROM_TO_PATTERN = "%s - %s";
  private String START_TIME_DATE_PATTERN = "%s 00:00";
  private String END_TIME_DATE_PATTERN = "%s 23:59";
  private int DAY_10_OF_MONTH = 10;
  private int DAY_20_OF_MONTH = 20;

  @BeforeEach
  void startProcess() {
    open(EngineUrl.createProcessUrl("process-analyzer/1910BF871CE43293/startAnalytic.ivp"));
  }

  @Test
  public void checkTimeIntervalDropdownExists() {
    $(By.id("process-analytics-form:filter-types")).shouldBe(visible);
  }

  @Test
  public void checkDefaultTimeIntervalFilterType() {
    $(By.id("process-analytics-form:filter-types_label")).shouldBe(visible).shouldBe(exactText("Today"));
    String today = dateFormat.format(new Date());
    $(By.id("process-analytics-form:date-point-selection_input")).shouldBe(visible).shouldHave(value(today));
  }

  @Test
  public void testTimeIntervalFilter() {
    // Test Yesterday type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Yesterday']")).shouldBe(visible).click();
    LocalDate yesterday = LocalDate.now().minusDays(1);
    $(By.id("process-analytics-form:date-point-selection_input")).shouldBe(visible)
        .shouldHave(value(yesterday.format(dateTimeFormatter)));

    // Test Today type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Today']")).shouldBe(visible).click();
    $(By.id("process-analytics-form:date-point-selection_input")).shouldBe(visible)
        .shouldHave(value(dateFormat.format(new Date())));

    // Test Custom type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Custom']")).shouldBe(visible).click();
    String startTimeToday = String.format(START_TIME_DATE_PATTERN, dateFormat.format(new Date()));
    $(By.id("process-analytics-form:between-date-from_input")).shouldBe(visible).shouldHave(value(startTimeToday));
    String endTimeToday = String.format(END_TIME_DATE_PATTERN, dateFormat.format(new Date()));
    $(By.id("process-analytics-form:between-date-to_input")).shouldBe(visible).shouldHave(value(endTimeToday));

    // Test Between type
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Between']")).shouldBe(visible).click();
    $(By.id("process-analytics-form:date-range_input")).shouldBe(visible).shouldBe(exactText("")).click();
    $(By.id("process-analytics-form:date-range_panel")).shouldBe(visible);
    selectDayOnDateRange(DAY_10_OF_MONTH);
    selectDayOnDateRange(DAY_20_OF_MONTH);
    LocalDate day10 = LocalDate.now().withDayOfMonth(DAY_10_OF_MONTH);
    LocalDate day20 = LocalDate.now().withDayOfMonth(DAY_20_OF_MONTH);
    String expectation =
        String.format(FROM_TO_PATTERN, day10.format(dateTimeFormatter), day20.format(dateTimeFormatter));
    $(By.id("process-analytics-form:date-range_input")).shouldBe(visible).shouldHave(value(expectation));
  }

  private void openFilterTypes() {
    var typeFilter = $(By.id("process-analytics-form:filter-types"));
    typeFilter.shouldBe(visible);
    Selenide.sleep(1000);
    typeFilter.click();
    $(By.id("process-analytics-form:filter-types_panel")).shouldBe(visible, Duration.ofSeconds(1));
  }

  private void selectDayOnDateRange(int day) {
    ElementsCollection links =
        $$(By.cssSelector("div[id='process-analytics-form:date-range_panel'] table tbody tr td a"));
    for (WebElement link : links) {
      if (link.getText().equals(String.valueOf(day))) {
        link.click();
        return;
      }
    }
  }

}
