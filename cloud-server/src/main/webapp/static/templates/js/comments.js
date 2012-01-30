
var commentUrl;

function showForumReply() {
    $('#forumReply').dialog({
        modal: true,
        width: 500,
        title: "Post a new comment",
        buttons: { 
            "Ok": function() { 
                sendNewForumComment('newComment','comments');
                $(this).dialog("close"); 
            } ,
            "Cancel": function() {
                $(this).dialog("close"); 
            }
        }
    });
    $('#newComment').focus();
}


function sendNewForumComment( commentId, containerId) {
    var comment = $("#" + commentId).val();
    log('sendNewForumComment', commentUrl, comment, commentId,  $("#" + commentId));	
    var url = commentUrl + "_DAV/PROPPATCH";
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: url,
        data: "clyde:newComment=" + comment,
        dataType: "text",
        success: function() {
            ajaxLoadingOff();
            $("#" + commentId).val('');
            addComment(containerId, userName, now(), comment);
			$('#forumReply').dialog("close");
        },
        error: function() {
            ajaxLoadingOff();
            alert('Sorry, we could process your comment. Please try again later');
        }
    });
}

function loadComments(page, containerId) {
    commentUrl = page;
    var url = page + "_DAV/PROPFIND?fields=clyde:comments&depth=0";
    var container = $("#" + containerId);
    if( $("#" + containerId + ":visible").length == 0) {
        return;
    }
    container.text('');
    $.getJSON(url, function(response) {
        var comments = response[0].comments;
        if( comments ) {
            comments.sort( dateOrd );
            for( i=0; i<comments.length; i++ ) {
                var comment = comments[i];
                addComment(containerId, comment.user.name, comment.date, comment.comment);
            }
        }
    });
}

function addComment(containerId, userName, commentDate, comment ) {
    var s = "";
    s = s + "<p class='forumAnnotation'>" + userName + " | " + toDisplayDateNoTime(commentDate) + "</p>";
    s = s + "<p class='forumText'>" + comment + "</p>";
    var container = $("#" + containerId);
    container.prepend("<div class='forumComment'>" + s + "</div>");

}
