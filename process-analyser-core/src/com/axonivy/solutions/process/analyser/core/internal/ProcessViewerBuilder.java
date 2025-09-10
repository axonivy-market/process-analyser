package com.axonivy.solutions.process.analyser.core.internal;

import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.AND;
import static com.axonivy.solutions.process.analyser.core.constants.ProcessAnalyticsConstants.SLASH;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.APP;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.FILE;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.HIGHLIGHT;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.PMV;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.PROCESS_MINER_FILE;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.SELECT;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.SERVER;
import static com.axonivy.solutions.process.analyser.core.constants.ViewerConstants.ZOOM;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.bo.Process;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.htmldialog.IHtmlDialogContext;
import ch.ivyteam.ivy.security.ISecurityContext;

public class ProcessViewerBuilder {

  private final Map<String, String> queryParams = new HashMap<>();
  private final String contextPath;

  public ProcessViewerBuilder(Process selectedProcess) {
    IApplication application = IApplication.current();
    contextPath = application.getContextPath();
    setQueryParam(SERVER, detectServerParam());
    setQueryParam(APP, application.getName());
    setQueryParam(PMV, selectedProcess.getPmvName());
    setQueryParam(FILE, selectedProcess.getProjectRelativePath());
  }

  private String detectServerParam() {
    String server = IHtmlDialogContext.current().applicationHomeLink().toAbsoluteUri().getAuthority();
    String securityContextName = ISecurityContext.current().getName();
    if (!ISecurityContext.DEFAULT.equals(securityContextName)) {
      server = StringUtils.join(server, SLASH, securityContextName);
    }
    return server;
  }

  public ProcessViewerBuilder fitToScreen(boolean fitToScreen) {
    return addQueryParam("fitToScreen", Boolean.toString(fitToScreen));
  }

  public ProcessViewerBuilder highlight(String elementId) {
    return addQueryParam(HIGHLIGHT, elementId);
  }

  public ProcessViewerBuilder select(String elementIds) {
    return addQueryParam(SELECT, elementIds);
  }

  public ProcessViewerBuilder zoom(int zoom) {
    return setQueryParam(ZOOM, String.valueOf(zoom));
  }

  public URI toURI() {
    var uriBuilder = UriBuilder.fromPath(contextPath)
        .path("faces")
        .path("view")
        .path(IProcessModel.current().getName())
        .path(PROCESS_MINER_FILE);
    for (var queryParam : queryParams.entrySet()) {
      uriBuilder = uriBuilder.queryParam(queryParam.getKey(), queryParam.getValue());
    }
    return uriBuilder.build();
  }

  private ProcessViewerBuilder setQueryParam(String name, String value) {
    queryParams.put(name, value);
    return this;
  }

  private ProcessViewerBuilder addQueryParam(String name, String value) {
    queryParams.compute(name, (nam, val) -> {
      return StringUtils.isBlank(val) ? value : val.concat(AND).concat(value);
    });
    return this;
  }

}
