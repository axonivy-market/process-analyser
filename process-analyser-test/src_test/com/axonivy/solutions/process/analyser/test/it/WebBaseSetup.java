package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.time.Duration;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import org.apache.commons.lang3.StringUtils;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class WebBaseSetup {
  private final String ANALYZING_PROCESS_PATH = "process-analyser/1910BF871CE43293/startAnalytic.ivp";
  private final String LOGIN_URL = "/process-analyser-test/1973F53724EE655A/login.ivp?username=Developer&password=Developer";
  private final String CHANGE_LANGUAGE_LOCALE = "/process-analyser-test/1973F53724EE655A/changeLocale.ivp?locale=";
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

  protected void login() {
    open(EngineUrl.createProcessUrl(LOGIN_URL));
  }

  protected void changeLocaleToGerman() {
    open(EngineUrl.createProcessUrl(CHANGE_LANGUAGE_LOCALE + Locale.GERMAN.getLanguage()));
  }

  protected void resetLocale() {
    open(EngineUrl.createProcessUrl(CHANGE_LANGUAGE_LOCALE + Locale.ENGLISH.getLanguage()));
  }

  protected void login() {
    open(EngineUrl.createProcessUrl(LOGIN_URL));
  }

  protected void changeLocaleToGerman() {
    open(EngineUrl.createProcessUrl(CHANGE_LANGUAGE_LOCALE + Locale.GERMAN.getLanguage()));
  }

  protected void resetLocale() {
    open(EngineUrl.createProcessUrl(CHANGE_LANGUAGE_LOCALE + Locale.ENGLISH.getLanguage()));
  }

  protected void verifyAndClickItemLabelInDropdown(String dropdownCssSelector, String labelText,
      String dropdownListSuffix, String dropdownLabelSuffix) {
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
        .shouldBe(visible, Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));
    targetElement.click();
    $(dropdownCssSelector + dropdownLabelSuffix).shouldBe(Condition.text(labelText),
        Duration.ofSeconds(DEFAULT_TIMEOUT_DURATION));
  }
}
