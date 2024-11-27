package com.axonivy.utils.process.analyzer.test.filter;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.text.SimpleDateFormat;
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

@IvyWebTest
public class TimeIntervalFilterTest {
  private static SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.DATE_PATTERN);
  private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateUtils.DATE_PATTERN);
  private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DateUtils.DATE_TIME_PATTERN);

  @BeforeEach
  void startProcess() {
    open(EngineUrl.createProcessUrl("/process-analyzer/1910BF871CE43293/startAnalytic.ivp"));
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
  public void checkYesterdayFilter() {
    openFilterTypes();
    LocalDate yesterday = LocalDate.now().minusDays(1);
    $(By.cssSelector("li[data-label^='Yesterday']")).shouldBe(visible).click();
    $(By.id("process-analytics-form:date-point-selection_input")).shouldBe(visible)
        .shouldHave(value(yesterday.format(dateTimeFormatter)));
  }

  @Test
  public void checkBetweenFilter() {
    openFilterTypes();
    $(By.cssSelector("li[data-label^='Between']")).shouldBe(visible).click();
    $(By.id("process-analytics-form:date-range_input")).shouldBe(visible).shouldBe(exactText("")).click();
    $(By.id("process-analytics-form:date-range_panel")).shouldBe(visible);
    selectDayOnDateRange("10");
    selectDayOnDateRange("20");
    LocalDate day10 = LocalDate.now().withDayOfMonth(10);
    LocalDate day20 = LocalDate.now().withDayOfMonth(20);
    String expectation = day10.format(dateTimeFormatter) + " - " + day20.format(dateTimeFormatter);
    
    $(By.id("process-analytics-form:date-range_input")).shouldBe(visible).shouldHave(value(expectation));
  }
  
  private void selectDayOnDateRange(String day) {
    ElementsCollection links = $$(By.cssSelector("div[id='process-analytics-form:date-range_panel'] table tbody tr td a"));
    for (WebElement link : links) {
        if (link.getText().equals(day)) {
            link.click();
            return;
        }
    }
  }

  public void openFilterTypes() {
    $(By.id("process-analytics-form:filter-types")).shouldBe(visible).click();
    $(By.id("process-analytics-form:filter-types_items")).shouldBe(visible);
  }
  
}
