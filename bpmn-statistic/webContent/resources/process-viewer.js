function getCenterizeButton() {
  return $("#process-analytic-viewer").contents().find("#fitToScreenBtn");
}

function updateUrlForIframe() {
  const dataUrl = $("[id$='process-analytics-form:hidden-image']").attr("src");
  const encodedDataUrl = encodeURIComponent(dataUrl);
  const currentViewerUrl = $("[id$='process-analytic-viewer']").attr("src");
  const url = currentViewerUrl + "&miningUrl=" + encodedDataUrl;
  $("[id$='process-analytic-viewer']").attr("src", url);
}

function captureIframe() {
  centerizeIframeImages();
  captureScreenFromIframe("process-analytic-viewer");
}

function captureScreenFromIframe(id) {
  const iframe = $(`#${id}`)[0];
  html2canvas(iframe.contentWindow.document.body)
    .then((canvas) => {
      const imgData = canvas.toDataURL("image/png");
      const outputImage = document.getElementById("output-image");
      outputImage.src = imgData;
    })
    .catch((err) => {
      console.error("Error capturing iframe content:", err);
    });
}

function centerizeIframeImages() {
  const returnToCenterBtn = getCenterizeButton();
  if (returnToCenterBtn) {
    returnToCenterBtn.click();
    setTimeout(500);
  } else {
    console.error("Button not found!");
  }
}
