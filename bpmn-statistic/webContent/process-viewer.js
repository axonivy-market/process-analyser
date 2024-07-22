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
  const sprotty = getProcessDiagramIframe().find("#sprotty");
  if (isAdditionalInformationNotRendered(sprotty)) {
    sprotty.append(createBarWithText(innerText));
  }
}

function isAdditionalInformationNotRendered(sprottyNode) {
  return sprottyNode.find("#additional-information").length != 1;
}

function createText(innerText) {
  var newText = document.createElementNS("http://www.w3.org/1999/xhtml", "text");
  newText.setAttributeNS(null, "class", "sprotty-label label addtional-information");
  var textNode = document.createTextNode(innerText);
  newText.appendChild(textNode);
  return newText;
}

function createBarWithText(text) {
  let bar = createDivWithClass("ivy-viewport-bar");
  bar.setAttribute("id", "additional-information")
  bar.setAttribute("style", "left: 1rem; right: auto");
  let innerBox = createDivWithClass("viewport-bar");
  let innerText = createText(text);
  innerBox.appendChild(innerText);
  bar.appendChild(innerBox)
  return bar;
}

function createDivWithClass(cssClass) {
  let div = document.createElementNS("http://www.w3.org/1999/xhtml", "div");
  div.setAttribute("class", cssClass);
  return div;
}
