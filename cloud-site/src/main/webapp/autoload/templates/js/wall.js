var wallArr;

function initWall() {
    var url;
    var propfind;
    if( jsonDev ) {
        propfind = "DAV/PROPFIND.txt";
    } else {
        propfind = "_DAV/PROPFIND";
    }
    propfind = propfind + "?fields=clyde:wall&depth=0";
 
    var wallDiv = $("#wall");
    wallDiv.html("<img class='ajaxLoading' src='../templates/images/framework/ajax/loading-icon.gif' />"); // TODO ajaxloading

    // load thumbs
    $.getJSON(".." + accountRootPath() + propfind, function(response) {        
        if( response ){
            var wall1 = response[0].wall;
            loadWall(wall1);
        }
        
    });
    $.getJSON(userUrl + propfind, function(response) {
        if( response ){
            var wall2 = response[0].wall;
            loadWall(wall2);
        }

    });

}

function loadWall(wall) {
    if( wallArr ) {
        wallArr = wallArr.concat(wall);
    } else {
        wallArr = wall;
        return; // wait until both loaded
    }

    var wallDiv = $("#wall");
    wallDiv.html("");

    wallArr.sort(dateOrdDesc);

    for( i=0; i<wallArr.length && i < 50; i++) {
        var item = wallArr[i];

        var dt = toWallDate(item.lastUpdated);

        var folderPath = item.folderPath;
        var itemContent = "";
        if( item.type == "folder") {
            itemContent = showFolder(item);
            var folderName = $.URLDecode(getFileName(item.folderPath));
            folderPath = folderPath.substring(7);
            itemContent = itemContent + "<p>ShmeGO uploaded a set of files from <a href='fileman.html#" + folderPath + "'>" + folderName + "</a> on " + dt + "</p>";

        } else if( item.type == "sharedFolder" ) {
            itemContent = showFolder(item);
            var folderName = $.URLDecode(item.folderPath);
            itemContent = itemContent + "<p>" + item.fromUser + " shared a set of files from <a href='fileman.html#Shared/" + folderName + "'>" + folderName + "</a> on " + dt + "</p>";

        } else if( item.type == "sharedFile" ) {
            var fileName = $.URLDecode(item.filePath);
            itemContent = "<p>Moved to trash " + fileName + " at " + dt + "</p>";
        } else if( item.type == "trashed" ) {
            var fileName = $.URLDecode(item.filePath);
            itemContent = "<p>Moved to trash " + fileName + " at " + dt + "</p>";
        } else if( item.type == "deleted" ) {
            var nm = $.URLDecode(getFileName(item.filePath));
            itemContent = "<p>Deleted " + nm + " at " + dt + "</p>";
        } else {
            itemContent = "unknown event type";
        }
        // note we add the item.type into the wall item div to allow styling per type
        var itemHtml = "<div class='" + item.type + "Wall'>" + itemContent + "</div>";
        wallDiv.append(itemHtml);
    }
}

function showFolder(item) {
    var itemContent = "";
    for( j=0; j<item.updatedFiles.length && j<4; j++){
        var file = item.updatedFiles[j];
        if( file.thumbHref && file.thumbHref.length>0 ) {
            var thumbHref = file.thumbHref;
            itemContent = itemContent + "<img src='" + thumbHref  + "' alt='No thumbnail'/>";
        } else {
            if( file.href ) {
                var iconHref = "../images/ml/fileManager/icons/" + findIconByExt(file.href);
                var fName = $.URLDecode(getFileName(file.href));
                var thumbFrame = "<div><img src='" + iconHref + "' alt=' ' />";
                thumbFrame = thumbFrame + "<p>" + fName + "</p></div>";
                itemContent = itemContent + thumbFrame;
            }
        }
    }
    return itemContent;
}

function dateOrdDesc(n, m) {
    var i = dateOrd(n,m);
    return i * -1;
}

function dateOrd(n, m) {
    if( n == null ) {
        if( m == null ) {
            return 0;
        } else {
            return 1;
        }
    } else {
        if( m == null ) {     
            return -1;
        } else {
            var dt1 = n.lastUpdated;
            var dt2 = m.lastUpdated;
            if( dt1.year == dt2.year ) {
                if( dt1.month == dt2.month) {
                    if( dt1.date == dt2.date ) {
                        if( dt1.hours == dt2.hours) {
                            return dt1.minutes - dt2.minutes;
                        } else {
                            return dt1.hours - dt2.hours;
                        }
                    } else {
                        return dt1.date - dt2.date;
                    }
                } else {
                    return dt1.month - dt2.month;
                }
            } else {
                return dt1.year - dt2.year;
            }
        }
    }
}

function toWallDate(dt) {
    if( dt ) {
        var month = dt.month; // month is zero indexed
        month++;
        var s = dt.date + "/" + month + "/" + (dt.year+1900) + " at " + pad2(dt.hours) + ":" + pad2(dt.minutes);
        return s;
    } else {
        return "";
    }
    
}


function pad2(i) {
    if( i < 10 ) {
        return "0" + i;
    } else {
        return i;
    }
}
