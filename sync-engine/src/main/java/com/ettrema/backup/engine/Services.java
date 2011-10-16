package com.ettrema.backup.engine;

import com.ettrema.context.ClassNotInContextException;
import com.ettrema.context.RootContext;

/**
 *
 * @author brad
 */
public class Services {
    public static <T> T _(Class<T> c) throws ClassNotInContextException{
        if( services == null ) {
            return null;
        }
        return services.rootContext.get(c);
    }
    
    private static Services services;
    
    public static void initInstance(RootContext rootContext) {
        services = new Services(rootContext);
    }
    
    private RootContext rootContext;
    
    private Services(RootContext rootContext) {
        this.rootContext = rootContext;
    }

}
