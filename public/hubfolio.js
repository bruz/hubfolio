$('.ui.label').popup();

function checkStatus() {
  var url = location.href + '/status';

  $.get(url).done(function(response) {
    if(response == 'generating') {
      setTimeout(function() {
        checkStatus();
      }, 5000);
    } else {
      location.reload();
    }
  });
};
