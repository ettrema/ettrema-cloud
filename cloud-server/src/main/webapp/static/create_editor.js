function create( id ) {
    var oFCKeditor = new FCKeditor( id ) ;
    oFCKeditor.BasePath	= '/static/' ;
    oFCKeditor.Height	= '100%' ;
    oFCKeditor.Width	= '100%' ;
    oFCKeditor.ReplaceTextarea() ;
}

function create( id,width,height,toolbarSet ) {
    var oFCKeditor = new FCKeditor( id ) ;
    oFCKeditor.BasePath	= '/static/' ;
    oFCKeditor.Height	= height || '100%';
    oFCKeditor.Width	= width  || '100%';
    oFCKeditor.ToolbarSet = toolbarSet;
    oFCKeditor.ReplaceTextarea() ;
}

jQuery(function($) {
    $(".htmleditor").each(function(i,n) {
        var inp = $(n);        
        var h = inp.attr("rows") ? inp.attr("rows") * 18 : null;
        var w = inp.attr("cols") ? inp.attr("cols") * 8 : null;

        var inputClasses = inp.attr("class");
        if(inputClasses) {
            c = inputClasses.split(" ");
            for( i=0; i<c.length; i++  ) {
                var s = c[i];
                if( s.startsWith("toolbar-")) {
                    s = s.substring(8);
                    toolbar = s;
                    break;
                }
            }    
        }

        create(inp.attr("id"), w, h, toolbar);
    });
});
