$(document).ready(function(){
  $('.ui.label').popup();

  $('.check-status').click(function() {
    location.reload();
  });
});

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
