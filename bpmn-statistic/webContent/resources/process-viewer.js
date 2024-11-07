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

async function getDiagramData() {
  await returnToFirstLayer();
  await centerizeIframeImage();
  await captureScreenFromIframe();
}

async function captureScreenFromIframe() {
  const iframe = queryObjectById(DIAGRAM_IFRAME_ID)[0];
  await hideViewPortBar(true);
  await updateMissingCssForChildSelector();
  await html2canvas(iframe.contentWindow.document.body, { allowTaint: true })
    .then((canvas) => {
      const imagenName = queryObjectByIdInForm(CURRENT_PROCESS_LABEL).text();
      const encodedImg = canvas.toDataURL(DEFAULT_IMAGE_TYPE);
      const link = document.createElement(ANCHOR_TAG);
      link.id = "tmp-anchor";
      link.href = encodedImg;
      link.download = `${imagenName}.jpeg`;
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
      "display": "flex",
      "justify-content": "center",
      "height": "100%",
    });
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
    await wait(DEFAULT_SLEEP_TIME_IN_MS);
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
