const JUMP_OUT_BTN_CLASS = "ivy-jump-out";
const DIAGRAM_IFRAME_ID = "process-analytic-viewer";
const FIT_TO_SCREEN_BUTTON_ID = "fitToScreenBtn";
const DEFAULT_SLEEP_TIME_IN_MS = 750;

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
  const dataUrl = queryObjectByIdInForm("hidden-image").attr("src");
  const encodedDataUrl = encodeURIComponent(dataUrl);
  const currentViewerUrl = $("[id$='process-analytic-viewer']").attr("src");
  const url = currentViewerUrl + "&miningUrl=" + encodedDataUrl;
  $("[id$='process-analytic-viewer']").attr("src", url);
}

async function getDiagramData() {
  await returnToFirstLayer();
  await centerizeIframeImage();
  await captureScreenFromIframe();
}

async function captureScreenFromIframe() {
  const iframe = queryObjectById(DIAGRAM_IFRAME_ID)[0];
  await html2canvas(iframe.contentWindow.document.body)
    .then((canvas) => {
      const encodedImg = canvas.toDataURL("image/jpeg");
      const link = document.createElement("a");
      link.id = "haha"
      link.href = encodedImg;
      link.download = "diagram.jpeg";
      link.click();
    })
    .catch((err) => {
      console.error("Error capturing iframe content:", err);
    });
}

function getJumpOutBtn() {
  return queryObjectById(DIAGRAM_IFRAME_ID)
    .contents()
    .find(buildClassRef(JUMP_OUT_BTN_CLASS))[0];
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
