var noTabs = 3;

function selectTab(selectedIndex) {

    for (var index = 1; index <= noTabs; index++) {
        var tab = document.getElementById('tab' + index);
        anchor = document.getElementById('anchor' + index);

        if (index != selectedIndex) {

            anchor.onclick = function (e) {
                var spanId = e.target.id;
                var spanIndex = spanId.charAt(spanId.length - 1);
                selectTab(spanIndex);

                return false;
            };

            anchor.className = 'not-selected';
            tab.style.visibility = 'hidden';
        } else {
            anchor.onclick = function (e) {
                return false;
            };
            anchor.className = 'selected';
            tab.style.visibility = 'visible';
        }
    }
}