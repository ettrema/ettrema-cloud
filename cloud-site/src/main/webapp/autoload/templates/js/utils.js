
var userUrl = null;
var userName = null;

jQuery(function($) {
    log("init the page");
    initUser();
    initButtons();
    initPageUploads();
});

function initButtons() {
    log('initButtons');
    $("button").not(".ui-button").wrapInner("<span class='ui-button-text'></span>");
    $("button").not(".ui-button").addClass("ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only");
    $("input").not(".ui-widget-content").addClass("ui-widget-content");
}


function initLoginPage() {
    var isPreviousLogin = window.location.href.indexOf("isLogin") > 0;
    if( isPreviousLogin ) {
        $("#validationMessage").html("Login failed, please check your details");
    } else {
        if( userUrl ) {
            $("#welcomeMessage").html("You don't have permission to access this page. If you are incorrectly logged in you can re-login here");
        } else {
            $("#welcomeMessage").html("You Must login to access this page.");
        }
    }
}

function createAccount() {
    try {
        doCreateAccount();
        return false;
    } catch(e) {
        log("exception", e);
        alert('There was an error creating your account: ' + e);
        return false;
    }
}
function doCreateAccount() {
    resetValidation();
    var container = $("#registerForm");

    if( !checkRequiredFields(container) ) {
        log('validation failed');
        alert("Some fields havent been entered correctly, please check and try again.");
        return;
    }


    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: '/users/_autoname.new/.ajax',
        data: $("#registerForm").serialize(),
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
            log('success');
            if( resp.result ) {
                log('result');
                if( resp.result == "ok") {
                    window.location = "/signupThanks.html";
                    return;
                } else {
                    if( Object.keys ) {
                        var msg = "Couldnt create your account for the following reason(s):\n";
                        keys = Object.keys(resp);
                        for( i=0; i<keys.length; i++) {
                            var key = keys[i];
                            if( key != "result") {
                                var m = resp[key];
                                msg += key + " - " + m + "\n";
                            }
                        }
                        alert(msg);
                    } else {
                        alert("We're sorry, but there was an error creating your account. Please check what you've entered and try again");
                    }
                }
            } else {
                alert("An error has occured and your application might not have been submitted");
            }

        },
        error: function() {
            ajaxLoadingOff();
            alert('There was an error creating your application. This might be because of a problem with your internet connection');
        }
    });
}

function setAccountRejected(isDisabled, container) {
    log('setAccountRejected', isDisabled, container);
    ajaxLoadingOn();
    $.ajax({
        type: 'POST',
        url: "_DAV/PROPPATCH",
        data: "clyde:rejectedApplication=" + isDisabled,
        dataType: "json",
        success: function(resp) {
            ajaxLoadingOff();
            if( resp.length == 0 ) {
                var newClass = isDisabled ? "disabled" : "enabled";
                log('update state:', $("div", container), newClass);
                $("div", container).attr("class",newClass);
                alert("The account has been rejected and deleted");
                window.location = "../";
            } else {
                alert("The user could not be updated because: " + resp[0].description);
            }
        },
        error: function(resp) {
            ajaxLoadingOff();
            log(failed, resp);
            alert("Sorry, the account could not be updated. Please check your internet connection");
        }
    });
}
function initPageUploads() {
    log("init uploads");
    //	initAjaxUploads();
    $('#fileDropContainer').dragUploadable(".", "picd", {
        dragleaveClass: "dragleave",
        dragenterClass: "dragenter",
        dropListing: "#fileDropListing",
        onUploaded: onUploaded
    });
    log("done multi uploads init");
}
function onUploaded(file){
    log("onUploaded", file);
// should reload files, but need ajax display
}
