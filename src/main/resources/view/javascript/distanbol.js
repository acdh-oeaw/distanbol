function setGenericImage(element) {
    var parentDiv = element.parentElement.parentElement;
    parentDiv.innerHTML = "<div><img id='thumbnailLink' src='/view/image/noImage.png'/></div>";
}

function addFadeToTextNav() {
    var textTab = document.getElementById("textTab");
    textTab.classList.add("fade");
}

function updateConfidenceSpan(element) {
    var output = element.nextSibling.nextSibling;//i have no idea why it needs the next sibling twice but it works.
    output.innerHTML = element.value;
}

//to update confidence span on load
var elements = document.querySelectorAll('[type="range"]');
for(var i=0;i<elements.length;i++){
    var output = elements[i].nextSibling.nextSibling;//i have no idea why it needs the next sibling twice but it works.
    output.innerHTML = elements[i].value;
}

