var setupKeyboardNav = function () {
    // disable tab access for all elements
    $$('input,select,textarea,a').each(function (e) {
        $(e).writeAttribute('tabindex', -1);
    });
    var tabindex = 1;

    var applyTabIndexes = function (e) {
        // enable tabbing to input, select, textarea and button links
        e.select('input,select,textarea,.PageButton').each(function (e) {
            if (e.type != 'hidden') {
                e.writeAttribute('tabindex', tabindex);
                tabindex++;
            }
        })
    }

    var enableHotkeys = function (e) {
        // reset any existing hotkey definitions
        if (Hotkeys != undefined) {
            Hotkeys.hotkeys = [];
        }
        // enable hotkeys for elements with a "data-er-hotkey" attribute
        e.select("[data-er-hotkey]").each(function (e) {
            if (e.type != 'hidden') {
                Hotkeys.bind(e.readAttribute("data-er-hotkey"), function () {
                    e.click();
                });
            }
        })
    }

    // find sibling divs of embedded overlays
    var embeddedBlocks = $$('.EmbeddedOverlay + div');
    if (embeddedBlocks.length > 0) {
        // embedded blocks are always nested, so it's sufficient to select the
        // last
        var lastEmbeddedBlock = embeddedBlocks[embeddedBlocks.length - 1];
        if (lastEmbeddedBlock != undefined) {
            // enable tabbing to elements in frontmost embedded block
            applyTabIndexes(lastEmbeddedBlock);
            enableHotkeys(lastEmbeddedBlock);
        }
        // no embedded blocks on the page
    } else if ($$('form.PageForm').length == 1) {
        // enable tabbing to current form's elements
        applyTabIndexes($$('form.PageForm')[0]);
        enableHotkeys($$('form.PageForm')[0]);
    }
};
