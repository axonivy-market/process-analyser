package com.axonivy.solutions.process.analyser.core.internal;

import static com.axonivy.solutions.process.analyser.core.constants.CoreConstants.AND;
import static com.axonivy.solutions.process.analyser.core.constants.CoreConstants.SLASH;
import static com.axonivy.solutions.process.analyser.core.enums.ViewerParam.*;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.solutions.process.analyser.core.enums.ViewerParam;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.htmldialog.IHtmlDialogContext;
import ch.ivyteam.ivy.security.ISecurityContext;

public class ProcessViewerBuilder {

  private static final String PARAM_TEMPLATE = "{%s}";
  private final Map<ViewerParam, String> queryParams = new HashMap<>();
  private final String contextPath;

  public ProcessViewerBuilder() {
    IApplication application = IApplication.current();
    contextPath = application.getContextPath();
    setQueryParam(SERVER, detectServerParam());
    setQueryParam(APP, application.getName());
  }

  private String detectServerParam() {
    String server = IHtmlDialogContext.current().applicationHomeLink().toAbsoluteUri().getAuthority();
    String securityContextName = ISecurityContext.current().getName();
    if (!ISecurityContext.DEFAULT.equals(securityContextName)) {
      server = StringUtils.join(server, SLASH, securityContextName);
    }
    return server;
  }

  public ProcessViewerBuilder pmv(String pmvName) {
    return setQueryParam(PMV, pmvName);
  }

  public ProcessViewerBuilder projectPath(String projectRelativePath) {
    return setQueryParam(FILE, projectRelativePath);
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
        .path(FACES.getValue())
        .path(VIEW.getValue())
        .path(IProcessModel.current().getName())
        .path(PROCESS_MINER_FILE.getValue());
    // Build URI with template e.g /uri/param={param}
    List<String> queryParamKeys = queryParams.keySet().stream()
        .sorted(Comparator.comparingInt(ViewerParam::ordinal))
        .map(ViewerParam::getValue).toList();
    for (var queryParam : queryParamKeys) {
      uriBuilder = uriBuilder.queryParam(queryParam, PARAM_TEMPLATE.formatted(queryParam));
    }
    return uriBuilder.buildFromMap(queryParams.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().getValue(), Map.Entry::getValue)));
  }

  private ProcessViewerBuilder setQueryParam(ViewerParam param, String value) {
    queryParams.put(param, value);
    return this;
  }

  private ProcessViewerBuilder addQueryParam(ViewerParam param, String value) {
    queryParams.compute(param, (key, val) -> {
      return StringUtils.isBlank(val) ? value : val.concat(AND).concat(value);
    });
    return this;
  }

}
