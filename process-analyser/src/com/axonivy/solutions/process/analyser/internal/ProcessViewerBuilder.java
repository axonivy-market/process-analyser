package com.axonivy.solutions.process.analyser.internal;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import static com.axonivy.solutions.process.analyser.constants.CoreConstants.AND;
import static com.axonivy.solutions.process.analyser.constants.CoreConstants.SLASH;
import com.axonivy.solutions.process.analyser.enums.ViewerParam;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.APP;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.FACES;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.FILE;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.HIGHLIGHT;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.PMV;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.PROCESS_MINER_FILE;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.SELECT;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.SERVER;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.VIEW;
import static com.axonivy.solutions.process.analyser.enums.ViewerParam.ZOOM;

import ch.ivyteam.ivy.application.app.Application;
import ch.ivyteam.ivy.application.project.Project;
import ch.ivyteam.ivy.htmldialog.IHtmlDialogContext;
import ch.ivyteam.ivy.security.ISecurityContext;
import jakarta.ws.rs.core.UriBuilder;

public class ProcessViewerBuilder {

  private static final String PARAM_TEMPLATE = "{%s}";
  private final Map<ViewerParam, String> queryParams = new HashMap<>();
  private final String contextPath;

  public ProcessViewerBuilder() {
    Application application = Application.current();
    contextPath = application.contextPath();
    setQueryParam(SERVER, detectServerParam());
    setQueryParam(APP, application.name());
  }

  private String detectServerParam() {
    String server = IHtmlDialogContext.current().appHomeLink().toAbsoluteUri().getAuthority();
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
        .path(Project.current().name())
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
    queryParams.compute(param, (_, val) -> {
      return StringUtils.isBlank(val) ? value : val.concat(AND).concat(value);
    });
    return this;
  }

}
