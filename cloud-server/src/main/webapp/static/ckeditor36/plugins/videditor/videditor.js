var oEditor    = window.parent.InnerDialogLoaded(); 
var FCK        = oEditor.FCK; 
var FCKLang    = oEditor.FCKLang ;
var FCKConfig  = oEditor.FCKConfig ;
var oMovie     = null;
var isNew	   = true;
var flashPlayer =  FCKConfig.PluginsPath + '/videditor/player_flv_maxi.swf';
var mp3Player =  FCKConfig.PluginsPath + '/videditor/player_mp3_maxi.swf';



/** Initial setup */
window.onload = function() { 
	// Translate dialog box
	oEditor.FCKLanguageManager.TranslatePage(document);
	// Load selected movie
	loadMovieSelection();
	// Show Ok button
	window.parent.SetOkButton(true);
}


/** Movie object */
var Movie = function (o){
	this.id = '';
	this.contentType = '';
	this.url = '';
	this.autoplay = '0';
	this.width = '320';
	this.height = '240';
	if(o) this.setObjectElement(o);
};


/** Load FCKEditor selection */
function loadMovieSelection() {
	oMovie = new Movie();
	var oSel = FCK.Selection.GetSelectedElement();
	if(oSel != null) {
		var oSelConv = FCK.GetRealElement(oSel);
		if(oSelConv != null) {
			oSel = oSelConv;
		}
		if (oSel.id != null && oSel.id.match(/^movie[0-9]*$/)) {
			oMovie.setAttribute('id', oSel.id);
		}
		for ( var i = 0; i < oSel.childNodes.length; i++) {
			if (oSel.childNodes[i].tagName == 'PARAM') {
				var name = GetAttribute(oSel.childNodes[i], 'name');
				var value = GetAttribute(oSel.childNodes[i], 'value');
				if (name == 'FlashVars') {
					// Flash video
					var vars = value.split('&');
					for ( var fv = 0; fv < vars.length; fv++) {
						var varsT = vars[fv].split('=');
						name = varsT[0];
						value = varsT[1];
						if (name == 'flv') {
							oMovie.setAttribute('url', value);
						} else {
							oMovie.setAttribute(name, value);
						}
					}
				} else if (name == 'autostart' || name == 'autoplay') {
					value = (value == 'true' || value == '1') ? 1 : 0;
					oMovie.setAttribute('autoplay', value);
				} else if (name == 'src'
						|| (name == 'movie' && !name.endsWith(flashPlayer))) {
					oMovie.setAttribute('url', value);
				} else {
					// Other movie types
					oMovie.setAttribute(name, value);
				}
				isNew = false;
			}
		}
	}
	
	// Read current settings (existing movie)
	GetE('txtUrl').value = oMovie.url;
	if(!isNew) {
		oMovie.contentType = getContentType(oMovie.url);
	}
	GetE('txtWidth').value = oMovie.width;
	GetE('txtHeight').value = oMovie.height;
	GetE('chkAutoplay').checked	= oMovie.autoplay == '1';
	
	// Show/Hide according to settings
	ShowE('tdBrowse', FCKConfig.LinkBrowser);
	return oMovie;
}


/** Browse/upload a file on server */
function BrowseServer() {
	if(!FCKConfig.MediaBrowserURL) {
		FCKConfig.MediaBrowserURL = FCKConfig.ImageBrowserURL.replace(/(&|\?)Type=Image/i, "$1Type=Media");
	}
	if(!FCKConfig.MediaBrowserWindowWidth) {
		FCKConfig.MediaBrowserWindowWidth = FCKConfig.ScreenWidth * 0.7;
	}
	if(!FCKConfig.MediaBrowserWindowHeight) {
		FCKConfig.MediaBrowserWindowHeight = FCKConfig.ScreenHeight * 0.7;
	}
	OpenFileBrowser(
			FCKConfig.MediaBrowserURL, 
			FCKConfig.MediaBrowserWindowWidth, 
			FCKConfig.MediaBrowserWindowHeight);
}


/** Start processing */
function Ok() {
	if(GetE('txtUrl').value.length == 0) {
		GetE('txtUrl').focus();
		window.parent.SetSelectedTab('Info');
		alert(FCKLang.MoviePlayerNoUrl) ;
		return false ;
	}
	
	oEditor.FCKUndo.SaveUndoStep();
	
	var e = (oMovie || new Movie()) ;
	updateMovieObject(e) ;
	if(!isNew) {
		if(!navigator.userAgent.contains('Safari')) {
			FCK.Selection.Delete();
		}
		FCK.InsertHtml(e.getInnerHTML());
	}else{
		FCK.InsertHtml(e.getInnerHTML());
	}
	
	return true;
}


/** Update Movie object from Form */
function updateMovieObject(e){
	e.url = GetE('txtUrl').value;
	e.contentType = getContentType(e.url);
	e.width = (isNaN(GetE('txtWidth').value)) ? 0 : parseInt(GetE('txtWidth').value);
	e.height = (isNaN(GetE('txtHeight').value)) ? 0 : parseInt(GetE('txtHeight').value);
	e.autoplay = (GetE('chkAutoplay').checked) ? '1' : '0';
}


/** Create Movie html */
Movie.prototype.getInnerHTML = function (objectId){
	var rnd = Math.floor(Math.random()*1000001);
	var s = "";

	// html
	if(this.contentType == "application/x-shockwave-flash") {
		if(getExtension(this.url) == 'flv') {
			// Flash video (FLV)
			s += '<OBJECT id="movie' + rnd + '" ';
			s += '        type="application/x-shockwave-flash" ';
			s += '        data="'+ flashPlayer +'" ';
			s += '        width="'+this.width+'" height="'+this.height+'" >';
		    s += '  <PARAM name="allowFullScreen" value="true" />';
		    s += '  <PARAM name="movie" value="'+ flashPlayer +'" />';
		    s += '  <PARAM name="FlashVars" value="flv='+this.url+'&amp;showplayer=always&amp;width='+this.width+'&amp;height='+this.height+'&amp;showiconplay=true&amp;autoplay='+this.autoplay+'&amp;showstop=1&amp;showvolume=1&amp;showtime=1&amp;showfullscreen=1&amp;bgcolor1=189ca8&amp;bgcolor2=a0befd&amp;playercolor=a0befd&amp;volume=100" />';
		    s += '</OBJECT>';
		    
		}else{
			// Fix youtube url
			if(this.url.contains('youtube.com/')) {
				this.url = this.url.replace(/youtube\.com\/watch\?v=/i, "youtube.com/v/");
			}
			
			// Flash object (SWF)
			s += '<OBJECT id="movie' + rnd + '" ';
			s += '        type="application/x-shockwave-flash" ';
			s += '        data="'+ this.url +'" ';
			s += '        width="'+this.width+'" height="'+this.height+'" >';
		    s += '  <PARAM name="movie" value="'+ this.url +'" />';
		    s += '  <PARAM name="FlashVars" value="autoplay='+this.autoplay+'" />';
		    s += '</OBJECT>';			
		}

	}else if (this.contentType == "audio/mpeg") {
		// We use the flash mp3 player 
			s += '<OBJECT id="movie' + rnd + '" ';
			s += '        type="application/x-shockwave-flash" ';
			s += '        data="'+ mp3Player +'" ';
			s += '        width="250" height="20" >';
		    s += '  <PARAM name="bgcolor" value="#ffffff" />';
		    s += '  <PARAM name="movie" value="'+ mp3Player +'" />';
		    s += '  <PARAM value="mp3='+this.url+'&amp;autoplay='+this.autoplay+'&amp;showstop=1&amp;showvolume=1&amp;bgcolor1=0ffff5&amp;bgcolor2=000029" name="FlashVars" />';
		    s += '</OBJECT>';
		

	}else {
		// Other video types
		var pluginspace, codebase, classid;
		if(this.contentType == "video/quicktime" || this.contentType == "video/mp4" || this.contentType == "video/mpeg" || this.contentType == "video/x-msvideo" ) {
			// QUICKTIME
			this.autoplay = (this.autoplay == 'true' || this.autoplay == '1') ? 'true' : 'false';
			s += '<OBJECT id="movie' + rnd + '" ';
			s += '        classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" ';
			s += '        codebase="http://www.apple.com/qtactivex/qtplugin.cab" '
			s += '        width="'+this.width+'" height="'+(this.height + 15)+'" >';
		        s += '  <PARAM name="src" value="'+ this.url +'" />';
			s += '  <PARAM name="autoplay" value="'+this.autoplay+'" />';
			s += '  <PARAM name="controller" value="true" />';
			s += '  <OBJECT type="'+this.contentType+'" ';
			s += '          data="'+ this.url +'" ';
			s += '          width="'+this.width+'" height="'+this.height+'" ';
//			s += '          style="*display:none">'; // for IE6 only
			s += '    <PARAM name="autoplay" value="'+this.autoplay+'" />';
			s += '    <PARAM name="controller" value="true" />';
		    s += '  </OBJECT>';
		    s += '</OBJECT>';	
		}else{
			// WINDOWS MEDIA & OTHERS
			this.autoplay = (this.autoplay == 'true' || this.autoplay == '1') ? 'true' : 'false';
			s += '<OBJECT id="MediaPlayer" ';
			s += '        classid="CLSID:22D6F312-B0F6-11D0-94AB-0080C74C7E95" ';
			s += '        standby="Loading Microsoft Windows Media Player components..." ';
			s += '        type="application/x-oleobject" ';
			s += '        codebase="http://activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=6,4,7,1112" ';
			s += '        width="'+this.width+'" height="'+this.height+'" >';
			s += '<param name="filename" value="'+ this.url +'" />';
			s += '<param name="autoStart" value="'+this.autoplay+'" />'; 
			s += '<param name="showControls" value="true" />';
			s += '<param name="ShowStatusBar" value="true" />';
			s += '<param name="ShowDisplay" value="false" />';
			s += '<embed src="'+this.url+'" type="video/x-msvideo" name="MediaPlayer" autoplay="'+this.autoplay+'" showcontrols="1" showdisplay="0" width="'+this.width+'" height="'+this.height+'" /> ';
		    s += '</OBJECT>';
		    
		}
	    
	}
	
	return s;
}


/** Set movie attribute */
Movie.prototype.setAttribute = function(attr, val) {
	if (val=="true") {
		this[attr]=true;
	} else if (val=="false") {
		this[attr]=false;
	} else {
		this[attr]=val;
	}
};

/** Get the file extension  */
function getExtension(url) {
	var ext = url.match(/\.(avi|asf|fla|flv|mov|mp3|mp4|m4v|mpg|mpeg|mpv|qt|swf|wma|wmv)$/i);
	if(ext != null && ext.length && ext.length > 0) {
		ext = ext[1];
	}else{
		if(url.contains('youtube.com/')) {
			ext = 'swf';
		}else{
			ext = '';
		}
	}
	return ext;
}

/** Configure content type basing on provided url */
function getContentType(url) {
	var ext = getExtension(url);	
	var contentType = 
			(ext=="mpg"||ext=="mpeg"||ext=="mpv") ? "video/mpeg":
			(ext=="mp4") ? "video/mp4":
			(ext=="m4v") ? "video/mp4":
			(ext=="mp3") ? "audio/mpeg":
			(ext=="flv"||ext=="fla"||ext=="swf") ? "application/x-shockwave-flash":
			(ext=="wmv"||ext=="wm") ? "video/x-ms-wmv":
                 	(ext=="avi") ? "video/x-msvideo":
			(ext=="asf") ? "video/x-ms-asf":				
			(ext=="wma") ? "audio/x-ms-wma":
			(ext=="mov"||ext=="qt") ? "video/quicktime" : "video/x-ms-wmv";
	return contentType;
}

Movie.prototype.setObjectElement = function (e){
	if (!e) return ;
	this.width = GetAttribute( e, 'width', this.width );
	this.height = GetAttribute( e, 'height', this.height );
};

String.prototype.endsWith = function(str) 
{return (this.match(str+"$")==str)}

String.prototype.contains = function(str) 
{return (this.match(str)==str)}

/** Set selected URL from Browser */
function SetUrl(url) {
	GetE('txtUrl').value = url;
}

