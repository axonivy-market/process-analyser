package com.axonivy.utils.process.analyzer.enums;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import ch.ivyteam.ivy.environment.Ivy;

public interface HasCmsName {
  static final String PACKAGE_CONNECTOR = "[.]";
  static final String UNDERSCORE = "_";
  static final String SLASH = "/";
  static final String ENUMS_CMS_BASE = SLASH + "Enums";
  static final String NAME_PROPERTY = "name";

  /**
   * Get the name of this instance.
   *
   * @return name of this instance.
   */
  String name();

  /**
   * Return the name entry of the instance.
   *
   * @return
   */
  default String getCmsName() {
    return getCms(NAME_PROPERTY);
  }

  /**
   * Lookup the entry for an instance in the Ivy CMS.
   *
   * If the entry is not found, then return the name of the entry.
   *
   * @param entry
   * @return CMS entry
   */
  default String getCms(String entry) {
    final String cmsPath = getEntryPath(entry);
    String result = Ivy.cms().co(cmsPath);
    if (isEmpty(result)) {
      Ivy.log().warn("Missing CMS entry for '" + cmsPath + "'");
      result = name().replace(UNDERSCORE, SPACE);
    }
    return result;
  }

  default String getCmsUrl(String entry) {
    final String cmsPath = getEntryPath(entry);
    String result = Ivy.cms().cr(cmsPath);
    if (isEmpty(result)) {
      Ivy.log().warn("Missing CMS entry for '" + cmsPath + "'");
      result = name();
    }
    return result;
  }

  default String getEntryPath(String entry) {
    return getBasePath() + SLASH + name() + SLASH + entry;
  }

  default String getBasePath() {
    return ENUMS_CMS_BASE + SLASH + getClass().getSimpleName().replaceAll(PACKAGE_CONNECTOR, SLASH);
  }

  default String getCmsURI() {
    return getEntryPath(NAME_PROPERTY);
  }
}
