var noTabs = 3;

function chooseTab(chosenIndex) {

    for (var index = 1; index <= noTabs; index++) {
        var tab = document.getElementById('tab' + index);
        span = document.getElementById('span' + index);

        if (index != chosenIndex) {

            span.onclick = function (e) {
                var spanId = e.target.id;
                var spanIndex = spanId.charAt(spanId.length - 1);
                chooseTab(spanIndex);
            };

            span.className = 'not-chosen';
            span.style.cursor = 'pointer';
            tab.style.visibility = 'hidden';
        } else {
            span.onclick = null;
            span.className = 'chosen';
            span.style.cursor = 'auto';
            tab.style.visibility = 'visible';
        }
    }
}