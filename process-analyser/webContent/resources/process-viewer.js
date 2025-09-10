const DIAGRAM_IFRAME_ID = "process-analytic-viewer";
const FIT_TO_SCREEN_BUTTON_ID = "fitToScreenBtn";
const DEFAULT_SLEEP_TIME_IN_MS = 500;
const SPROTTY_VIEWPORT_BAR_ID = "sprotty_ivy-viewport-bar";
const HIDDEN_CLASS = "hidden";
const CHILD_DIV_FROM_NODE_ELEMENT_SELECTOR = ".node-child-label > div";
const DEFAULT_IMAGE_TYPE = "image/jpeg";
const ANCHOR_TAG = "a";
const CURRENT_PROCESS_LABEL = "processDropdown_label";
const HIDDEN_IMAGE_ID = "hidden-image";
const JUMP_OUT_BTN_CLASS = "ivy-jump-out";
const IVY_PROCESS_EXTENSION = ".ivp";
const NODE_WITH_ICON_BESIDE_SELECTOR =
  "g[id*=_label]:has(~.activity-icon) .node-child-label>div";
const FULL_HD_RESOLUTION_WIDTH = "1920px";
const FULL_HD_RESOLUTION_HEIGHT = "1080px";
const DEFAULT_IFRAME_WIDTH = "100%";
const DEFAULT_IFRAME_HEIGHT = "400px";
const EXECUTED_CLASS = "executed";
const EXECUTED_CLASS_CSS_SELECTOR = "." + EXECUTED_CLASS;
const EXECUTION_BADGE_CSS_SELECTOR = ".execution-badge";
const COMPLETE = "complete";
const PID_QUERY_PARAM_NAME = "pid";
const SUB_PROCESS_CALL_PID = "subProcessCallPid";

function getCenterizeButton() {
  return queryObjectById(DIAGRAM_IFRAME_ID)
    .contents()
    .find(buildIdRef(FIT_TO_SCREEN_BUTTON_ID))[0];
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
  return $(`[id$='process-analytics-form:${id}']`);
}

function updateUrlForIframe() {
  const dataUrl = queryObjectByIdInForm(HIDDEN_IMAGE_ID).attr("src");
  const encodedDataUrl = encodeURIComponent(dataUrl);
  const currentViewerUrl = window.frames[DIAGRAM_IFRAME_ID].src;
  const url = currentViewerUrl + "&miningUrl=" + encodedDataUrl;
  window.frames[DIAGRAM_IFRAME_ID].src = url;
}

async function getFullDiagramData() {
  await returnToFirstLayer();
  //await setIframeResolution(2000, 2000);

  await centerizeIframeImage();
  await wait(DEFAULT_SLEEP_TIME_IN_MS);
  await centerizeIframeImage();
  await captureScreenFromIframe(true);
  // await setIframeResolution(DEFAULT_IFRAME_WIDTH, DEFAULT_IFRAME_HEIGHT);
}

async function getCurrentViewPortOfDiagramData() {
  //await returnToFirstLayer();
  //await setIframeResolution(2000, 2000);

  // await centerizeIframeImage();
  await wait(DEFAULT_SLEEP_TIME_IN_MS);
  // await centerizeIframeImage();
  await captureScreenFromIframe(false);
  // await setIframeResolution(DEFAULT_IFRAME_WIDTH, DEFAULT_IFRAME_HEIGHT);
}

async function captureScreenFromIframe(shouldResizeFrame) {
  await hideViewPortBar(true);
  await updateMissingCssForChildSelector();

  const iframe = document.querySelector("[id$='process-analytic-viewer']");
  const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
  const body = iframeDoc.body;

  const contentWidth = body.scrollWidth;
  const contentHeight = body.scrollHeight;
  if (shouldResizeFrame) {
    iframe.style.width = "2000px";
    iframe.style.height = "2000px";
  }

  await html2canvas(body, {
    scale: 2,
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

      if (shouldResizeFrame) {
        iframe.style.width = contentWidth + "px";
        iframe.style.height = contentHeight + "px";
      }
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
  iframe.width = width;
  iframe.height = height;
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

function loadIframe(recheckIndicator) {
  var iframe = document.getElementById(DIAGRAM_IFRAME_ID);

  if (recheckIndicator) {
    const iframeDoc = iframe.contentDocument;
    if (iframeDoc.readyState == COMPLETE) {
      santizeDiagram();
      clearTimeout(recheckFrameTimer);
      const iframeRootUrl = iframe.contentWindow.location.href;
      const pidValue = getPidQueryParamValue(iframeRootUrl);
      updateDataTable([{ name: SUB_PROCESS_CALL_PID, value: pidValue }]);
      return;
    }
  }
  recheckFrameTimer = setTimeout(function () {
    loadIframe(true);
  }, 500);
}

// 1. Helper: copy computed styles inline
function inlineComputedStyles(svg) {
  const clone = svg.cloneNode(true);

  const allElements = clone.querySelectorAll("*");
  allElements.forEach((el) => {
    const computed = window.getComputedStyle(el);
    let style = "";
    for (const key of computed) {
      style += `${key}:${computed.getPropertyValue(key)};`;
    }
    el.setAttribute("style", style);
  });

  return clone;
}
function replaceForeignObjects(svg) {
  const foList = svg.querySelectorAll("foreignObject");
  foList.forEach((fo) => {
    const div = fo.querySelector("div");
    if (div) {
      const text = document.createElementNS(
        "http://www.w3.org/2000/svg",
        "text"
      );
      text.setAttribute("x", fo.getAttribute("x") || 0);
      text.setAttribute("y", fo.getAttribute("y") || 14);
      text.textContent = div.textContent;
      fo.parentNode.replaceChild(text, fo);
    }
  });
}
// 2. Export as JPEG at given scale (e.g., 2 = 200%)
function exportSVGToJPEG(svgElement, scale = 2, filename = "image.jpg") {
  if (svgElement === undefined) {
    const iframe = document.querySelector("iframe");
    const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
    svgElement = iframeDoc.querySelector("svg.sprotty-graph");
  }
  const inlineSVG = inlineComputedStyles(svgElement);
  //replaceForeignObjects(svgElement);

  const serializer = new XMLSerializer();
  const source = serializer.serializeToString(inlineSVG);
  console.warn("svg : " + source);

  document.getElementById(
    "process-analytics-form:process-analytic-viewer-panel:svgData"
  ).value = inlineSVG.outerHTML;
  exportSvg();
}

function openViewerInNewTab() {
  var url = $("[id$='process-analytic-viewer']").prop("src");
  if (url) {
    window.open(url, "_blank");
  } else {
    alert("Viewer URL not found");
  }
}
