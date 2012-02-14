var noTabs = 3;

function selectTab(selectedIndex) {

    for (var index = 1; index <= noTabs; index++) {
        var tab = document.getElementById('tab' + index);
        span = document.getElementById('span' + index);

        if (index != selectedIndex) {

            span.onclick = function (e) {
                var spanId = e.target.id;
                var spanIndex = spanId.charAt(spanId.length - 1);
                selectTab(spanIndex);
            };

            span.className = 'not-selected';
            span.style.cursor = 'pointer';
            tab.style.visibility = 'hidden';
        } else {
            span.onclick = null;
            span.className = 'selected';
            span.style.cursor = 'auto';
            tab.style.visibility = 'visible';
        }
    }
}