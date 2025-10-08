const DIAGRAM_IFRAME_ID = "process-analytic-viewer";
const FIT_TO_SCREEN_BUTTON_ID = "fitToScreenBtn";
const CENTER_BUTTON_ID = "centerBtn";
const DEFAULT_SLEEP_TIME_IN_MS = 500;
const SPROTTY_VIEWPORT_BAR_ID = "sprotty_ivy-viewport-bar";
const HIDDEN_CLASS = "hidden";
const CHILD_DIV_FROM_NODE_ELEMENT_SELECTOR = ".node-child-label > div";
const DEFAULT_IMAGE_TYPE = "image/jpeg";
const ANCHOR_TAG = "a";
const CURRENT_PROCESS_LABEL = "process-analyser-dropdown";
const HIDDEN_IMAGE_ID = "process-mining-url-hidden-image";
const JUMP_OUT_BTN_CLASS = "ivy-jump-out";
const IVY_PROCESS_EXTENSION = ".ivp";
const NODE_WITH_ICON_BESIDE_SELECTOR =
  "g[id*=_label]:has(~.activity-icon) .node-child-label>div";
const FULL_HD_RESOLUTION_WIDTH = "1920px";
const FULL_HD_RESOLUTION_HEIGHT = "1080px";
const DEFAULT_IFRAME_WIDTH = "100%";
const DEFAULT_IFRAME_HEIGHT = "100%";
const EXECUTED_CLASS = "executed";
const EXECUTED_CLASS_CSS_SELECTOR = "." + EXECUTED_CLASS;
const EXECUTION_BADGE_CSS_SELECTOR = ".execution-badge";
const COMPLETE = "complete";
const PID_QUERY_PARAM_NAME = "pid";
const SUB_PROCESS_CALL_PID = "subProcessCallPid";
const MINING_URL_PARAM = "&miningUrl=";

function getCenterizeButton() {
  return queryObjectById(DIAGRAM_IFRAME_ID)
    .contents()
    .find(buildIdRef(CENTER_BUTTON_ID))[0];
}

function buildIdRef(id) {
  return `#${id}`;
}

function buildClassRef(cssClass) {
  return `.${cssClass}`;
}

function queryObjectById(id) {
  return $(buildIdRef(id));
}

function queryObjectByIdInForm(id) {
  return $(`[id$=':${id}']`);
}

function findElementById(id) {
  return $(`[id$='${id}']`)[0];
}

function updateUrlForIframe() {
  const dataUrl = queryObjectByIdInForm(HIDDEN_IMAGE_ID).attr("src");
  const encodedDataUrl = encodeURIComponent(dataUrl);
  const currentViewerUrl = window.frames[DIAGRAM_IFRAME_ID].src;
  let url;
  if (currentViewerUrl.includes(MINING_URL_PARAM)) {
    // If miningUrl param already exists, replace it
    url = currentViewerUrl.replace(MINING_URL_PARAM, MINING_URL_PARAM + encodedDataUrl);
  } else {
    // If miningUrl param doesn't exist, add it
    url = currentViewerUrl + MINING_URL_PARAM + encodedDataUrl;
  }
  window.frames[DIAGRAM_IFRAME_ID].src = url;
}

async function getFullDiagramData() {
  await wait(DEFAULT_SLEEP_TIME_IN_MS);
  await returnToFirstLayer();
  await setIframeResolution(FULL_HD_RESOLUTION_WIDTH, FULL_HD_RESOLUTION_HEIGHT);
  await wait(DEFAULT_SLEEP_TIME_IN_MS);
  await centerizeIframeImage();
  await captureScreenFromIframe();
  await setIframeResolution(DEFAULT_IFRAME_HEIGHT, DEFAULT_IFRAME_WIDTH);
}

async function getCurrentViewPortOfDiagramData() {
  await captureScreenFromIframe();
}

async function captureScreenFromIframe() {
  await hideViewPortBar(true);
  await updateMissingCssForChildSelector();

  const iframe = document.querySelector("[id$='process-analytic-viewer']");
  const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;

  await html2canvas(iframeDoc.body, {
    scale: 1,
    allowTaint: true,
  })
    .then((canvas) => {
      let imageName = queryObjectByIdInForm(CURRENT_PROCESS_LABEL).text();
      imageName = imageName.split(IVY_PROCESS_EXTENSION)[0];
      const encodedImg = canvas.toDataURL(DEFAULT_IMAGE_TYPE);
      const link = document.createElement(ANCHOR_TAG);
      link.href = encodedImg;
      link.download = `${imageName}.jpeg`;
      link.click();
    })
    .catch((err) => {
      console.error("Error capturing iframe content:", err);
    });
  await hideViewPortBar(false);
}

async function updateMissingCssForChildSelector() {
  getContentsById(DIAGRAM_IFRAME_ID)
    .find(CHILD_DIV_FROM_NODE_ELEMENT_SELECTOR)
    .css({
      "align-items": "center",
      display: "flex",
      "justify-content": "center",
      height: "100%",
    });
  getContentsById(DIAGRAM_IFRAME_ID)
    .find(NODE_WITH_ICON_BESIDE_SELECTOR)
    .css({ "margin-left": "40px", "text-align": "start" });
}

async function setIframeResolution(width, height) {
  const iframe = queryObjectById(DIAGRAM_IFRAME_ID)[0];
  iframe.style.width = width;
  iframe.style.height = height;
}

async function returnToFirstLayer() {
  if (getJumpOutBtn()) {
    await getJumpOutBtn().click();
    await wait(DEFAULT_SLEEP_TIME_IN_MS);
  }
}

async function centerizeIframeImage() {
  const returnToCenterBtn = getCenterizeButton();
  if (returnToCenterBtn) {
    await returnToCenterBtn.click();
    await wait(DEFAULT_SLEEP_TIME_IN_MS * 1.5);
  } else {
    console.error("Button not found!");
  }
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function hideViewPortBar(boolean) {
  const viewPortBar = getContentsById(DIAGRAM_IFRAME_ID).find(
    buildIdRef(SPROTTY_VIEWPORT_BAR_ID)
  );
  if (boolean) {
    viewPortBar.addClass(HIDDEN_CLASS);
  } else {
    viewPortBar.removeClass(HIDDEN_CLASS);
  }
}

function getContentsById(id) {
  return queryObjectById(id).contents();
}

function getJumpOutBtn() {
  return queryObjectById(DIAGRAM_IFRAME_ID)
    .contents()
    .find(buildClassRef(JUMP_OUT_BTN_CLASS))[0];
}

function removeExecutedClass() {
  getProcessDiagramIframe()
    .find(EXECUTED_CLASS_CSS_SELECTOR)
    .removeClass(EXECUTED_CLASS);
}

function removeDefaultFrequency() {
  getProcessDiagramIframe()
    .find(EXECUTION_BADGE_CSS_SELECTOR)
    .each(function () {
      $(this).parent().remove();
    });
}

function santizeDiagram() {
  removeDefaultFrequency();
  removeExecutedClass();
}

function getProcessDiagramIframe() {
  return queryObjectById(DIAGRAM_IFRAME_ID).contents();
}

function getPidQueryParamValue(url) {
  const parseUrl = new URL(url);
  return parseUrl.searchParams.get(PID_QUERY_PARAM_NAME);
}

function testtest() {
  reloadCaseAnalyitcs();
}

function loadIframe(recheckIndicator) {
  var iframe = document.getElementById(DIAGRAM_IFRAME_ID);

  if (recheckIndicator) {
    const iframeDoc = iframe.contentDocument;
    if (iframeDoc.readyState == COMPLETE) {
      santizeDiagram();
      clearTimeout(recheckFrameTimer);
      const iframeRootUrl = iframe.contentWindow.location.href;
      const pidValue = getPidQueryParamValue(iframeRootUrl);
      if (findElementById(":data-statistics") !== undefined) {
        updateDataTable([{ name: SUB_PROCESS_CALL_PID, value: pidValue }]);
      }
      return;
    }
  }
  recheckFrameTimer = setTimeout(function () {
    loadIframe(true);
  }, 500);
}

function openViewerInNewTab() {
  var url = $("[id$='process-analytic-viewer']").prop("src");
  if (url) {
    window.open(url, "_blank");
  } else {
    alert("Viewer URL not found");
  }
}

// Color picker popup handling
document.addEventListener("click", function (event) {
  var colorPickerWrapper = findElementById("color-picker-wrapper");
  var colorPickerComponent = findElementById(":color-picker");
  if (!colorPickerWrapper || !colorPickerComponent) {
    return;
  }
  var colorPickerWidget = PF("colorPickerWidget");
  if (colorPickerWidget && !colorPickerWrapper.contains(event.target)) {
    colorPickerWrapper.style.display = "none";
  }
});
