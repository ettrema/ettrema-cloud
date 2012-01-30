
function showMigrator() {
	log("showMigrator");
	$('#migrator').dialog({
		modal: true,
		width: 800,
		minHeight: 400
	}
	)
	initStatus();
}

/**
 *
 */
function initStatus() {
	$.ajax({
		type: "GET",
		dataType: "json",
		data: "fields=clyde:status",
		url: "migrator",
		success: function(response){
			log('got status response', response);
			if( response && response.statuses ) {
				showStatus(response[0].status);
			} else {
				$("#headline").html("No migration in progress");
				$("#migrateFiles tbody").html("<tr><td colspan='5'>No files</td></tr>");
				buttonControl(null);
			}
		},
		error: function(response) {
            
		}
	});
}

/**
 * Query for file changes
 */
function queryFiles() {
	log("queryFiles");
	ajaxLoadingOn();
	$.ajax({
		type: "GET",
		data: "fields=clyde:files",
		dataType: "json",
		url: "migrator",
		success: function(response){
			ajaxLoadingOff();
			log('got migrator response', response);
			$("#headline").html("Migration query results");
			showQuery(response[0].files);
		},
		error: function(response) {
			ajaxLoadingOff();
		}
	});
}

function showQuery(report) {
	log('showQuery', report.statuses.length);
	var tbody = $("#migrateFiles tbody");
	tbody.html("");
	for( i=0; i<report.statuses.length; i++) {
		var status = report.statuses[i];
		log("status", status, i);
		var tr = $("<tr>");
		tr.append("<td><input onclick='toggleMigrateFiles(this)' " + checkedStatus(status) + " type='checkbox' name='resourceId_" + i + "' id='resourceId_" + i + "' value='" + status.localId + "'/></td>");
		tr.append("<td><label for='resourceId_" + i + "'>" + status.localHref + "</label></td>");
		tr.append("<td>" + toDisplayDate(status.localModDate) + "</td>");
		tr.append("<td>" + toDisplayDate(status.remoteMod) + "</td>");
		tr.append("<td>" + status.comment + "</td>");
		tbody.append(tr);
	}
	$("#btnMigrateScan").hide();
	$("#btnMigrateRefresh").show();
	$("#btnMigrateStart").show();
	$("#btnMigrateStop").hide();
	$("#selectAll").show();
}


var timerStatus;

function checkedStatus(status) {
	if( isModified(status)) {
		return "checked='true'";
	} else {
		return "";
	}
}

/**
 * return true if the local mod date is after the remote mod date
 */
function isModified(status) {
	l = status.localModDate; // local
	r = status.remoteMod; // remote
	if(r == null) {
		return true;
	}
	if(l.year != r.year) {
		return l.year > r.year;
	} else {
		if( l.month != r.month) {
			return l.month > r.month;
		} else {
			if( l.date != r.date ) {
				return l.date > r.date;
			} else {
				if( l.hours != r.hours ) {
					return l.hours > r.hours;
				} else {
					if( l.minutes != r.minutes ) {
						return l.minutes > r.minutes;
					}
				}
			}
		}
	}
	return false;
}

function showStatus(report) {
	log('showReport - ', report);

	if( report.finished ) {
		$("#headline").html("Migration complete: "  + report.destHost);
	} else {
		var perc = Math.round(report.statuses.length * 100 / report.numSourceIds);
		$("#headline").html("Migration is running: " + report.destHost + " - " + perc + "%");
	}

	var tbody = $("#migrateFiles tbody");
	tbody.html("");
	for( i=0; i<report.statuses.length; i++) {
		var status = report.statuses[i];
		var tr = $("<tr>");
		tr.append("<td></td>");
		tr.append("<td>" + status.localHref + "</td>");
		tr.append("<td>" + toDisplayDate(status.localModDate) + "</td>");
		tr.append("<td>" + toDisplayDate(status.remoteMod) + "</td>");
		tbody.append(tr);
		if( status.comment ) {
			tr = $("<tr>");
			tr.append("<td></td>");
			tr.append("<td></td>");
			tr.append("<td colspan='3' class='error'>" + status.comment + "</td>");
			tbody.append(tr);
		}
	}

	if( !report.finished ) {
		timerStatus = window.setTimeout(function() {
			initStatus();
		},2000);
	}
	buttonControl(report);
	$("#selectAll").hide();
}

function buttonControl(report) {
	log('buttonControl');
	if( report ) {
		log('has report');
		if( report.finished ) {
			$("#btnMigrateScan").show();
			$("#btnMigrateRefresh").show();
			$("#btnMigrateStart").hide();
			$("#btnMigrateStop").hide();
		} else {
			// job running
			$("#btnMigrateScan").hide();
			$("#btnMigrateRefresh").show();
			$("#btnMigrateStart").hide();
			$("#btnMigrateStop").show();
		}
	} else {
		log("no report, so none running");
		$("#btnMigrateScan").show();
		$("#btnMigrateRefresh").show();
		$("#btnMigrateStart").hide();
		$("#btnMigrateStop").hide();
	}
}

function startMigration() {
	log('startMigration');
	clearTimeout(timerStatus);
	ajaxLoadingOn();
	$.ajax({
		type: "POST",
		data: $("#migrateForm").serialize(),
		dataType: "json",
		url: "migrator",
		success: function(response){
			ajaxLoadingOff();
			log('got migrator response', response);
			showStatus(response);
		},
		error: function(response) {
			ajaxLoadingOff();
		}
	});
}

function stopMigration() {
	log('stopMigration');
	ajaxLoadingOn();
	$.ajax({
		type: "POST",
		data: "command=stop",
		url: "migrator",
		success: function(response){
			ajaxLoadingOff();
			log('got migrator response', response);
		},
		error: function(response) {
			ajaxLoadingOff();
		}
	});
}

function toggleMigrateFiles(source) {
	log('toggleMigrateFiles', source);
	var isChecked = $(source).is(":checked");
	var startUrl = getAssociatedUrl(source);
	$("#migrateFiles input[type=checkbox]").each(function(index, node){
		var id = $(node).attr("id");
		var label = $("label[for=" + id + "]");
		var thisUrl = getAssociatedUrl(node);
		
		if( thisUrl.startsWith(startUrl) ) {
			if( isChecked ) {
				$(node).attr("checked", "checked");
			} else {
				$(node).removeAttr("checked");
			}
			
		}
	});
}

function getAssociatedUrl(input) {
	var id = $(input).attr("id");
	var label = $("label[for=" + id + "]");
	return label.text();
}

function toDisplayDate(dt) {
	if( dt ) {
		return (dt.date+1) + "/" + (dt.month+1) + "/" + (dt.year+1900) + " " + dt.hours + ":" + dt.minutes;
	} else {
		return "";
	}
}