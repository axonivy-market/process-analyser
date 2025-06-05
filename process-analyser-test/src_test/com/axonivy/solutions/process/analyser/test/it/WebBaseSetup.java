package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class WebBaseSetup {
  private final String ANALYZING_PROCESS_PATH = "process-analyser/1910BF871CE43293/startAnalytic.ivp";
  private final String LOGIN_URL = "/process-analyser-test/1973F53724EE655A/login.ivp?username=Developer&password=Developer";
  private final String CHANGE_LOCALE_TO_GERMAN = "/process-analyser-test/1973F53724EE655A/changeLocale.ivp?locale=de";

  private final String DROPDOWN_LIST_SUFFIX = "_items";
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

  protected void changeLocaleToGerman() {
    open(EngineUrl.createProcessUrl(CHANGE_LOCALE_TO_GERMAN));
  }

  protected void verifyAndClickItemLabelInDropdown(String dropdownCssSelector, String labelText,
      String dropdownListSuffix) {
    // Click target drop down when it's ready
    var dropdown = $(dropdownCssSelector);
    dropdown.shouldBe(visible, Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));
    dropdown.click();

    String dropdownListCssSelector = dropdownCssSelector + dropdownListSuffix;
    $(dropdownListCssSelector).shouldBe(visible, Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));

    // Find 1st option (index = 1 to avoid choosing default initial option of null)
    SelenideElement targetElement = $$(dropdownListCssSelector + " li").stream()
        .filter(item -> labelText.equals(item.text())).findAny()
        .orElseThrow(() -> new AssertionError(
            String.join(StringUtils.SPACE, "Dropdown item with text", labelText, "not found!")))
        .shouldBe(Condition.visible);
    targetElement.click();
  }
}
