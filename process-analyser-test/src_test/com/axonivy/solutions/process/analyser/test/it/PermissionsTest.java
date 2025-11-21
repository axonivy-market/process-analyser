package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;

@IvyWebTest
public class PermissionsTest extends WebBaseSetup {

  private static final String GUEST_USER = "GuestUser";
  private static final String MANAGER_USER = "ManagerUser";

  @Test
  void testShouldNotSeeProcessAnalyserContentDueToLackOfRole() {
    loginByGivenUserAndStartProcessAnalyser(GUEST_USER);
    var notPermittedForm = $("[id$='view-not-permitted-form']").shouldBe(visible, DEFAULT_DURATION);
    notPermittedForm.find("[id$='warning-message']").shouldBe(visible, DEFAULT_DURATION);
  }

  @Test
  void testCanOpenProcessAnalyserWithRequiredRole() {
    loginByGivenUserAndStartProcessAnalyser(MANAGER_USER);
    var processAnalyserForm = $("[id$='process-analytics-form']").shouldBe(visible, DEFAULT_DURATION);
    processAnalyserForm.find("[id$='process-analytic-viewer-panel:viewer-group']").shouldBe(visible, DEFAULT_DURATION);
  }

  private void loginByGivenUserAndStartProcessAnalyser(String username) {
    login(username, username);
    startAnalyzingProcess();
  }
}
