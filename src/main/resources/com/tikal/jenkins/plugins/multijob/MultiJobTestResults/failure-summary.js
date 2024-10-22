document.addEventListener("DOMContentLoaded", function () {
    const showLink = document.getElementById("showLink");
    showLink.addEventListener("click", showFailures);
});

function showFailures() {
    var elms = document.getElementsByClassName("hidden");
    for (var i = 0; i < elms.length; i++) {
        elms[i].style.display = "";
    }
    elm = document.getElementById("showLink");
    elm.style.display = "none";
}
