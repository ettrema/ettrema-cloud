// All about sharing folders and stuff

jQuery(function($) {
    initSharing();
});
 
function initSharing() {
	log("initSharing");
	$("#shareFolder").click(function() {
		displayShares();
		$("#shareFolderModal").dialog({
			modal: true,
			width: 500,
			title: "Share this folder with a friend",
			buttons: { 				
				"Ok": function() { 
					$(this).dialog("close"); 
					var friend = $("#shareFolderModal select[name=otherName]").val();
					var writable = $("#shareFolderModal input[name=writable]:checked").length > 0;
					shareFolder(friend, writable);					
				} ,
				"Cancel": function() {
					$(this).dialog("close"); 
				}
			}
		});		
	});
	
	$("button.import").click(function() {
		var path = $(this).attr("value");
		$("#importFolderModal").dialog({
			modal: true,
			width: 500,
			title: "Import folder to your files",
			buttons: { 				
				"Ok": function() { 
					$(this).dialog("close"); 					
					var destFolder = $("#importFolderModal select").val();
					var newName = $("#importFolderModal input").val();
					importFolder(path, destFolder + newName);
				} ,
				"Cancel": function() {
					$(this).dialog("close"); 
				}
			}
		});		
	});
}


function importFolder(path, newPath) {
    log('importFolder', path, newPath);
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: path,
        data: {
			_action: "bind",
			url: newPath
		},
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
        },
        error: function(resp) {
            ajaxLoadingOff();
            log("failed to enable account", resp);
            alert("Sorry, the account could not be updated. Please check your internet connection");
        }
    });	
}


function displayShares() {
    log('displayShares');
    ajaxLoadingOn();
    $.ajax({
        type: 'GET',
        url: "../_DAV/PROPFIND",
        data: {
			fields: "clyde:shares",
			depth: 0
		},
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
			var shares = resp[0].shares;
			log("shares", shares);
			var table = $("#shareFolderModal table");
			$("tbody",table).html("");
			for( i=0; i<shares.length; i++) {
				log("share", shares[i]);
				var share = shares[i];
				$("tbody",table).append("<tr><td>" + share.user + "</td><td>" + share.roles + "</td></tr>");
			}
        },
        error: function(resp) {
            ajaxLoadingOff();
            log("failed to enable account", resp);
            alert("Sorry, the account could not be updated. Please check your internet connection");
        }
    });
    
}

function shareFolder(friend, writable) {
    log('shareFolder', friend, writable);
	var role = "VIEWER";
	if( writable) {
		role = "AUTHOR";
	}
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: "..",
        data: {
			_action: "share",
			friend: friend,
			role: role
		},
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
        },
        error: function(resp) {
            ajaxLoadingOff();
            log("failed to enable account", resp);
            alert("Sorry, the account could not be updated. Please check your internet connection");
        }
    });
}