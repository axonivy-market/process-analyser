package com.axonivy.solutions.process.analyser.test.it;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import ch.ivyteam.ivy.security.ISecurityConstants;

@IvyWebTest
public class ProcessAnalyticsWebTest extends WebBaseSetup {

  private static final String SHOW_STATISTIC_BTN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:show-statistic-btn";
  private static final String MODULE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:module-dropdown";
  private static final String PROCESS_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:process-dropdown";
  private static final String KPI_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:kpi-dropdown";
  private static final String ROLE_DROPDOWN_CSS_SELECTOR = "#process-analytics-form\\:standard-filter-panel-group\\:role-dropdown";
  private static final String CASCADE_DROPDOWN_LIST_SUFFIX = "_panel";
  private static final String CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX = " .ui-cascadeselect-label";
  private static final String DISABLE_PROPERTY = "disabled";
  private static final String CHECK_PROPERTY = "checked";
  private static final String PROCESS_FILE = "TestProcess";
  private static final String PROCESS_NAME_EN = "Test process";
  private static final String PROCESS_NAME_DE = "Testprozess";
  private static final String FREQUENCY_OPTION_NAME = "Frequency";
  private static final String DROPDOWN_LABEL_SUFFIX = "_label";
  private static final String TEST_MODULE_NAME = "process-analyser-test";
  private static final String DROPDOWN_LIST_SUFFIX = "_items";
  private static final String MERGE_PROCESS_STARTS_INPUT_SELECTOR = "[id$=':additional-feature:merge-process-starts_input']";
  private static final String MERGE_PROCESS_STARTS_SELECTOR = "[id$=':additional-feature:merge-process-starts']";
  private static final String STANDARD_FILTER_PANEL_GROUP = "process-analytics-form:standard-filter-panel-group:";
    private static final String TREE_TABLE_SELECTOR = "table#process-analytics-form\\:statistic-viewer\\:node";
  private static final String TREE_TABLE_ROWS_SELECTOR = TREE_TABLE_SELECTOR + " tbody tr";
  private static final String TREE_TABLE_ROW_BY_KEY_SELECTOR = TREE_TABLE_SELECTOR + " tbody tr[data-rk='%s']";
  private static final String DATA_PARENT_KEY_ATTR = "data-prk";
  private static final String CLASS_ATTR = "class";
  private static final String ID_ATTR = "id";
  private static final String ROOT_PARENT_KEY = "root";
  private static final String NODE_ID_PATTERN = "node_[0-9](_[0-9])*";
  private static final String NODE_ID_PREFIX = "node_";

  @Test
  void showStatisticButtonShouldEnableWhenChosenFulfilled() throws InterruptedException {
    login();
    resetLocale();
    startAnalyzingProcess();
    verifyMergeProcessStartToggle();
    turnOffProcessStart();
    verifyMergeProcessStartToggleEmpty();
    // Check the current status of show statistic button
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    // Choose test project PM
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    Selenide.sleep(2000);
    // Verify English process name is rendered
    verifyAndSelectAProcess(PROCESS_NAME_EN);
    verifyAndClickItemLabelInDropdown(KPI_DROPDOWN_CSS_SELECTOR, FREQUENCY_OPTION_NAME, CASCADE_DROPDOWN_LIST_SUFFIX,
        CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX);
    // Choose system user role
    verifyAndClickItemLabelInDropdown(ROLE_DROPDOWN_CSS_SELECTOR, ISecurityConstants.SYSTEM_USER_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    // Check the status of show statistic button after data fulfilled
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, StringUtils.EMPTY));

    // Change locale
    changeLocaleToGerman();

    startAnalyzingProcess();
    turnOffProcessStart();
    verifyMergeProcessStartToggleEmpty();
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);
    Selenide.sleep(2000);
    // Verify German process name is rendered
    verifyAndSelectAProcess(PROCESS_NAME_DE);
    resetLocale();
  }

  @Test
  void showPmvWhenSelectProcess() {
    login();
    startAnalyzingProcess();
    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(attribute(DISABLE_PROPERTY, "true"));

    $(By.id(STANDARD_FILTER_PANEL_GROUP + "module-dropdown")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "module-dropdown_1")).click();
    Selenide.sleep(2000);
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmv-dropdown")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmv-dropdown_1")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmv-dropdown")).click();
    $(By.id(STANDARD_FILTER_PANEL_GROUP + "pmv-dropdown_2")).click();
  }

  private void verifyAndSelectAProcess(String startElementName) {
    // Verify and click the process drop-down
    var processDropdown = $(PROCESS_DROPDOWN_CSS_SELECTOR).shouldBe(visible, DEFAULT_DURATION);
    processDropdown.click();

    // Verify selection panel
    var selectionPanel = $(PROCESS_DROPDOWN_CSS_SELECTOR + CASCADE_DROPDOWN_LIST_SUFFIX).shouldBe(visible, DEFAULT_DURATION);

    // Find 1st option (index = 1 to avoid choosing default initial option of null)
    var processFileAndStartName = PROCESS_FILE.concat("/").concat("Start: ").concat(startElementName);
    SelenideElement targetElement = selectionPanel.$$(" li").stream()
        .filter(item -> processFileAndStartName.equals(item.text())).findAny()
        .orElseThrow(() -> new AssertionError(getDropdownItemNotFoundMessage(processFileAndStartName)))
        .shouldBe(visible, DEFAULT_DURATION);
    targetElement.click();
  }

  private void verifyMergeProcessStartToggle() {
    var toggle = $(MERGE_PROCESS_STARTS_INPUT_SELECTOR);
    toggle.shouldBe(attribute(CHECK_PROPERTY, "true"));
  }

  private void turnOffProcessStart() {
    var toggle = $(MERGE_PROCESS_STARTS_SELECTOR);
    toggle.click();
  }

  private void verifyMergeProcessStartToggleEmpty() {
    var toggle = $(MERGE_PROCESS_STARTS_INPUT_SELECTOR);
    toggle.shouldBe(attribute(CHECK_PROPERTY, StringUtils.EMPTY));
    $(PROCESS_DROPDOWN_CSS_SELECTOR).shouldBe(visible, DEFAULT_DURATION);
  }

  @Test
  void treeTableShouldRenderHierarchyCorrectly() throws InterruptedException {
    login();
    resetLocale();
    startAnalyzingProcess();
    verifyMergeProcessStartToggle();
    turnOffProcessStart();
    verifyMergeProcessStartToggleEmpty();
    verifyAndClickItemLabelInDropdown(MODULE_DROPDOWN_CSS_SELECTOR, TEST_MODULE_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);

    verifyAndSelectAProcess(PROCESS_NAME_EN);

    verifyAndClickItemLabelInDropdown(KPI_DROPDOWN_CSS_SELECTOR, FREQUENCY_OPTION_NAME, CASCADE_DROPDOWN_LIST_SUFFIX,
        CASCADE_DROPDOWN_LABEL_CSS_SELECTOR_SUFFIX);

    verifyAndClickItemLabelInDropdown(ROLE_DROPDOWN_CSS_SELECTOR, ISecurityConstants.SYSTEM_USER_NAME, DROPDOWN_LIST_SUFFIX,
        DROPDOWN_LABEL_SUFFIX);

    $(SHOW_STATISTIC_BTN_CSS_SELECTOR).shouldBe(visible, DEFAULT_DURATION).click();
    Selenide.sleep(3000);
    $(TREE_TABLE_SELECTOR).shouldBe(visible, DEFAULT_DURATION);

    verifyParentChildRelationship();
    verifyHierarchyLevels();
    verifyNodeIdPattern();
  }

  private void verifyParentChildRelationship() {
    var treeRows = $$(TREE_TABLE_ROWS_SELECTOR);

    treeRows.forEach(row -> {
      String parentRowKey = row.getAttribute(DATA_PARENT_KEY_ATTR);

      if (!ROOT_PARENT_KEY.equals(parentRowKey)) {
        var parentRow = $(String.format(TREE_TABLE_ROW_BY_KEY_SELECTOR, parentRowKey));
        parentRow.should(visible);
      }
    });
  }

  private void verifyHierarchyLevels() {
    // Get all rows grouped by level
    var level1Rows = $$(TREE_TABLE_ROWS_SELECTOR + ".ui-node-level-1");
    var level2Rows = $$(TREE_TABLE_ROWS_SELECTOR + ".ui-node-level-2");
    var level3Rows = $$(TREE_TABLE_ROWS_SELECTOR + ".ui-node-level-3");

    // Verify that level 1 nodes have no parent (prk = root)
    level1Rows.forEach(row -> {
      row.shouldHave(attribute(DATA_PARENT_KEY_ATTR, ROOT_PARENT_KEY));
    });

    // Verify that level 2 nodes have a level 1 parent
    level2Rows.forEach(row -> {
      String parentKey = row.getAttribute(DATA_PARENT_KEY_ATTR);
      var parentRow = $(String.format(TREE_TABLE_ROW_BY_KEY_SELECTOR, parentKey));
      parentRow.shouldHave(attribute(CLASS_ATTR, "ui-node-level-1"));
    });

    // Verify that level 3 nodes have a level 2 parent
    level3Rows.forEach(row -> {
      String parentKey = row.getAttribute(DATA_PARENT_KEY_ATTR);
      var parentRow = $(String.format(TREE_TABLE_ROW_BY_KEY_SELECTOR, parentKey));
      parentRow.shouldHave(attribute(CLASS_ATTR, "ui-node-level-2"));
    });
  }

  private void verifyNodeIdPattern() {
    var treeRows = $$(TREE_TABLE_ROWS_SELECTOR);

    treeRows.forEach(row -> {
      String rowId = row.getAttribute(ID_ATTR);
      String nodePattern = rowId.substring(rowId.lastIndexOf(NODE_ID_PREFIX));

      if (nodePattern.matches(NODE_ID_PATTERN)) {
        row.should(visible);
      }
    });
  }
}
