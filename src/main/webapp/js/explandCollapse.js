function expandCollapse(img) {
    var td = img.parentNode;
    var isExpand = td.getAttribute("isExpand");

    if (isExpand == "true") {
        resetSubTr(td, "childIds", "none");
        resetTdState(td, "false", "collpaseSrc");
    } else {
        resetSubTr(td, "directChildIds", "");
        resetTdState(td, "true", "expandSrc");
    }
}

function resetSubTr(td, childType, display) {
    var childIdArr = td.getAttribute(childType).split(",")

    for (var i = 0; i < childIdArr.length; i++) {
        var childTdId = childIdArr[i];
        $(childTdId).parentNode.style.display = display;

        if (childType == "directChildIds") {
            var collpaseSrc = $(childTdId).getAttribute("collpaseSrc");

            var tdChildNodes = $(childTdId).childNodes;
            for (var j = 0; j < tdChildNodes.length; j++) {
                var tdChildNode = tdChildNodes[j];
                if (tdChildNode.tagName == "IMG") {
                    tdChildNode.setAttribute("src", collpaseSrc);
                }
            }
        } else{
            $(childTdId).setAttribute("isExpand", "false");
        }
    }
}

function resetTdState(td, isExpand, srcType) {
    td.setAttribute("isExpand", isExpand);

    var src = td.getAttribute(srcType);
    for (var i = 0; i < td.childNodes.length; i++) {
        var tdChildNode = td.childNodes[i];
        if (tdChildNode.tagName == "IMG") {
            tdChildNode.setAttribute("src", src);
        }
    }
}

$("jenkins").onload = function (ev) {
    var td = $("@1");
    var img;
    for (var i = 0; i < td.childNodes.length; i++) {
        var tdChildNode = td.childNodes[i];
        if (tdChildNode.tagName == "IMG") {
            img = tdChildNode;
            break;
        }
    }
    expandCollapse(img);
    expandCollapse(img);
}
