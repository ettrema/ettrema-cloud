function create( id ) {
    var oFCKeditor = new FCKeditor( id ) ;
    oFCKeditor.BasePath	= '/' ;
    oFCKeditor.Height	= '100%' ;
    oFCKeditor.Width	= '100%' ;
    oFCKeditor.ReplaceTextarea() ;
}

function create( id,width,height,toolbarSet ) {
    var oFCKeditor = new FCKeditor( id ) ;
    oFCKeditor.BasePath	= '/' ;
    oFCKeditor.Height	= height || '100%';
    oFCKeditor.Width	= width  || '100%';
    oFCKeditor.ToolbarSet = toolbarSet;
    oFCKeditor.ReplaceTextarea() ;
}
