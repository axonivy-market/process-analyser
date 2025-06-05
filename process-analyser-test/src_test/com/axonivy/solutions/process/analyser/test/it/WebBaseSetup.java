package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Selenide.open;

import com.axonivy.ivy.webtest.engine.EngineUrl;

public class WebBaseSetup {
  private final String ANALYZING_PROCESS_PATH = "process-analyser/1910BF871CE43293/startAnalytic.ivp";
  private final String LOGIN_URL = "/process-analyser-test/1973F53724EE655A/login.ivp?username=Developer&password=Developer";

  protected void startAnalyzingProcess() {
    open(EngineUrl.createProcessUrl(ANALYZING_PROCESS_PATH));
  }

  protected void openProfilePage() {
	  open(EngineUrl.base()+"/dev-workflow-ui/faces/profile.xhtml");
  }
  
  protected void login() {
    open(EngineUrl.createProcessUrl(LOGIN_URL));

  }

}
