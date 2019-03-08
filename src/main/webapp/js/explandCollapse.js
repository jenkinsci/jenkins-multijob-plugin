function expandCollapse(img) {
    var td = img.parentNode;
    var isExpand = td.getAttribute("isExpand");


    if (isExpand == "true") {
        /**
         * 收起所有子元素
         */
        resetSubTr(td, "childIds", "none");
        resetTdState(td, "false", "collpaseSrc");
    } else {
        /**
         * 展开直接子元素
         */
        resetSubTr(td, "directChildIds", "");
        resetTdState(td, "true", "expandSrc");
    }
}

/**
 * 重置当前行的孩子行。
 * @param td
 * @param isExpand
 * @param childType
 * @param display
 */
function resetSubTr(td, childType, display) {
    var childIdArr = td.getAttribute(childType).split(",")

    for (var i = 0; i < childIdArr.length; i++) {
        var childTdId = childIdArr[i];
        $(childTdId).parentNode.style.display = display;

        if (childType == "directChildIds") {
            /**
             * 展开的直接子元素，设置为收起状态
             */
            var collpaseSrc = $(childTdId).getAttribute("collpaseSrc");

            var tdChildNodes = $(childTdId).childNodes;
            for (var j = 0; j < tdChildNodes.length; j++) {
                var tdChildNode = tdChildNodes[j];
                if (tdChildNode.tagName == "IMG") {
                    tdChildNode.setAttribute("src", collpaseSrc);
                }
            }
        } else{
            /**
             * 收起的所有子元素，都处于收起状态。
             */
            $(childTdId).setAttribute("isExpand", "false");
        }
    }
}

/**
 * 重置td的状态
 *
 * @param td 当前的td
 * @param isExpand td所在tr的状态（true:展开，false：收起）
 * @param srcType 图片显示类型（expandSrc: 展开时显示，collpaseSrc：收起时显示）
 */
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
