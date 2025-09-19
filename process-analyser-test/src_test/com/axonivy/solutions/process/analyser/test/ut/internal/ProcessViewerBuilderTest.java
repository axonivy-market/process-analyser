package com.axonivy.solutions.process.analyser.test.ut.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.axonivy.solutions.process.analyser.core.internal.ProcessViewerBuilder;
import com.axonivy.solutions.process.analyser.test.BaseSetup;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class ProcessViewerBuilderTest extends BaseSetup {

  @Test
  void test_buildBpmnIFrameSourceUrl() {
    var builder = new ProcessViewerBuilder();
    builder.pmv(SELECTED_MODULE_URL);
    builder.projectPath(SELECTED_PROJECT_PATH);
    assertThat(builder.toURI().toString()).isEqualTo(URI.create(TEST_IFRAME_SOURCE_URL).toString());
  }
}
