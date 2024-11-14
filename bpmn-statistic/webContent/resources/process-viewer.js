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
  await hideViewPortBar(true);
  await updateMissingCssForChildSelector();

  // Increase resolution for capturing image
  const iframe = await setIframeResolution("1920px", "1080px");
  const svgContent = iframe.contentWindow.document.body;

  await html2canvas(svgContent, {
    width: 1920,
    height: 1080,
    scale: 1,
    allowTaint: true
  }).then(canvas => {
    const imgData = canvas.toDataURL(DEFAULT_IMAGE_TYPE);
    let imageName = queryObjectByIdInForm(CURRENT_PROCESS_LABEL).text();
    imageName = imageName.split(IVY_PROCESS_EXTENSION)[0];
    const downloadLink = document.createElement(ANCHOR_TAG);
    downloadLink.href = imgData;
    downloadLink.download = `${imageName}.jpeg`;
    downloadLink.click();
  }).catch(error => {
    console.error("Capture failed:", error);
  });

  // Reset resolution for iframe
  await setIframeResolution("100%","400")
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

async function setIframeResolution(width, height) {
  const iframe = queryObjectById(DIAGRAM_IFRAME_ID)[0];
  iframe.style.width = width;
  iframe.style.height = height;
  return iframe;
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
