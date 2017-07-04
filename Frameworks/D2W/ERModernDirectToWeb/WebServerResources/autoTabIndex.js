var autoTabIndex = function() {
    // disable tab access for all elements
    $$('input,select,textarea,a').each(function(e) {
        $(e).writeAttribute('tabindex', -1);
    });
    var tabindex = 1;
    // helper function applying the actual change
    var apply = function(e) {
        // enable tabbing to input, select, textarea and button links
        e.select('input,select,textarea,.PageButton').each(function(e) {
            if (e.type != 'hidden') {
                e.writeAttribute('tabindex', tabindex);
                tabindex++;
            }
        }
        )
    }
    var embeddedBlocks = $$('.EmbeddedOverlay + div');
    if (embeddedBlocks.length > 0) {
        // embedded blocks are always nested, so it's sufficient to select the last
        var lastEmbeddedBlock = embeddedBlocks[embeddedBlocks.length - 1];
        if (lastEmbeddedBlock != undefined) {
            // enable tabbing to elements in frontmost embedded block
            apply(lastEmbeddedBlock);
        }
    } else if ($$('form.PageForm').length == 1) {
        // enable tabbing to current form's elements
        apply($$('form.PageForm')[0]);
    }
};

