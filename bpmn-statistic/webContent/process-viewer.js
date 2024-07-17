function removeExecutedClass() {
  getProcessDiagramIframe().find(".executed").removeClass("executed");
}

function removeDefaultFrequency() {
  getProcessDiagramIframe()
    .find(".execution-badge")
    .each(function () {
      $(this).parent().remove();
    });
}

function santizeDiagram() {
  removeDefaultFrequency();
  removeExecutedClass();
}

function getProcessDiagramIframe() {
  return $("#process-diagram-iframe").contents();
}

function addElementFrequency(elementId, frequencyRatio, backgroundColor, textColor) {
  getProcessDiagramIframe()
    .find(`#sprotty_${elementId}`)
    .append(
      `<svg>
        <g>
          <rect rx="7" ry="7" x="19" y="20" width="30" height="14" style="fill: rgb(${backgroundColor})"></rect>
          <text x="34" y="26" dy=".4em" style="fill: rgb(${textColor})">${frequencyRatio}</text>
        </g>
      </svg>`
    );
}

function loadIframe(recheckIndicator) {
  let iframe = document.getElementById("process-diagram-iframe");
  let recheckFrameTimer = setTimeout(function () {
    loadIframe(true);
  }, 500);

  if (recheckIndicator) {
    const iframeDoc = iframe.contentDocument;
    if (iframeDoc.readyState == "complete") {
      santizeDiagram();
      clearTimeout(recheckFrameTimer);
      return;
    }
  }
}

function renderAdditionalInformation(innerText) {
  const pool = getProcessDiagramIframe().find(".pool");
  if (pool != undefined && isAdditionalInformationRendered(pool)) {
    let rectPool = pool.find("rect.sprotty-node");
    let height = Number(rectPool.css("height").replace("px", "")) + 30;
    pool.append(prepareAdditionalInformationPanel(innerText, height));
  }
}

function isAdditionalInformationRendered(pool) {
  if (pool.find("rect.sprotty-node").find("addtional-information")) {
    return true;
  }
  return false;
}

function prepareAdditionalInformationPanel(innerText, top) {
  var svgNS = "http://www.w3.org/2000/svg";
  var newText = document.createElementNS(svgNS, "text");
  newText.setAttributeNS(null, "x", 150);
  newText.setAttributeNS(null, "class", "sprotty-label label addtional-information");
  newText.setAttributeNS(null, "y", top);
  var textNode = document.createTextNode(innerText);
  newText.appendChild(textNode);
  return newText;
}
