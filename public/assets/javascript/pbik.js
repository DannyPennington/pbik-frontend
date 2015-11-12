function scrollToError(id) {
    window.location.hash = '#' + id;
}

function contactHMRC() {
    var $errorContent = $('.report-error__content');
    $errorContent.removeClass('hidden');
    $errorContent.removeClass('js-hidden');
    scrollToElement('#get-help-action', 1000);
}

function scrollToElement(selector, time) {
    var $time = typeof(time) != 'undefined' ? $time : 800;
    var $verticalOffset = -10;
    var $selector = $(selector);
    var $offsetTop = $selector.offset().top + $verticalOffset;
    $('html, body').animate({
       scrollTop: $offsetTop
    }, $time);
}