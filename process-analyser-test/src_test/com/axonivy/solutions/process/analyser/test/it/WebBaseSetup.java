package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.time.Duration;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class WebBaseSetup {
  private final String ANALYZING_PROCESS_PATH = "process-analyser/1910BF871CE43293/startAnalytic.ivp";
  private final String LOGIN_URL = "/process-analyser-test/1973F53724EE655A/login.ivp?username=Developer&password=Developer";
  private final String DROPDOWN_LIST_SUFFIX = "_items";
  private final String DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX = DROPDOWN_LIST_SUFFIX + " li";
  protected final String DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = "_label";
  private final int DEFAULT_TIMEOUT_DURATION = 2;

  protected void startAnalyzingProcess() {
    open(EngineUrl.createProcessUrl(ANALYZING_PROCESS_PATH));
  }

  protected void openProfilePage() {
    open(EngineUrl.base() + "/dev-workflow-ui/faces/profile.xhtml");
  }

  protected void login() {
    open(EngineUrl.createProcessUrl(LOGIN_URL));

  }

  protected void clickOptionFromTheDropdownWithIndex(String dropdownCssSelector, int index) {
    // Click target drop down when it's ready
    var dropdown = $(dropdownCssSelector);
    dropdown.shouldBe(visible, Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));
    dropdown.click();
    $(dropdownCssSelector + DROPDOWN_LIST_SUFFIX).shouldBe(visible, Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));

    // Find 1st option (index = 1 to avoid choosing default initial option of null)
    SelenideElement targetElement = $$(dropdownCssSelector + DROPDOWN_ITEMS_CSS_SELECTOR_SUFFIX).get(index);
    String selectedOptionLabel = targetElement.text();
    targetElement.click();

    // Check if the label have been change to target option label
    $(dropdownCssSelector + DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX).shouldHave(Condition.text(selectedOptionLabel),
        Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));
  }

}
