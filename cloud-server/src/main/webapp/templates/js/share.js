// All about sharing folders and stuff

jQuery(function($) {
    initSharing();
});

function initSharing() {
	log("initSharing");
	$("#shareFolder").click(function() {
		$("#shareFolderModal").dialog({
			modal: true,
			width: 500,
			title: "Share this folder with a friend",
			buttons: { 
				"Ok": function() { 
					// TODO
				} ,
				"Cancel": function() {
					$(this).dialog("close"); 
				}
			}
		});		
	});
}