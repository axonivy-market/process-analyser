package com.axonivy.utils.process.analyzer.test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;

/**
 * This sample WebTest orchestrates a real browser to 
 * verify that your workflow application and especially it's Html Dialogs are running as expected.
 * 
 * <p>The test can either be run<ul>
 * <li>in the Designer IDE ( <code>right click > run as > JUnit Test </code> )</li>
 * <li>or in a Maven continuous integration build pipeline ( <code>mvn clean verify</code> )</li>
 * </ul></p>
 * 
 * <p>Detailed guidance on writing these kind of tests can be found in our
 * <a href="https://developer.axonivy.com/doc/11.3/concepts/testing/web-testing.html">WebTesting docs</a>
 * </p>
 */
@IvyWebTest
public class SampleWeb{

  @Test
  public void navigateToInfoPage(){
    open(EngineUrl.base());
    
    $("img").shouldBe(attribute("alt", "Logo"));
    $(By.tagName("img")).shouldBe(attribute("alt", "Logo"));
  }
}