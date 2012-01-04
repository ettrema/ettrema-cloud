package com.ettrema.backup.engine;

import com.ettrema.common.LogUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class StateTokenDaoImpl {

    private static final Logger log = LoggerFactory.getLogger(StateTokenDaoImpl.class);
    private Map<String, List<StateToken>> map = new ConcurrentHashMap<String, List<StateToken>>();

    public StateTokenDaoImpl() {
    }
    
    public List<StateToken> findForFolder(File parent) {
        List<StateToken> list = map.get(parent.getAbsolutePath());
        return list;
    }

    public void saveOrUpdate(StateToken token) {
        LogUtils.trace(log, "saveOrUpdate", token.filePath,token.currentCrc);
        File f = new File(token.filePath);
        List<StateToken> list = findForFolder(f.getParentFile());
        if (list == null) {
            list = new ArrayList<StateToken>();
            map.put(f.getParent(), list);
        }
        Iterator<StateToken> it = list.iterator();
        while (it.hasNext()) {
            StateToken t = it.next();
            if (t.filePath.equals(token.filePath)) {
                it.remove();
            }
        }
        list.add(token);
        LogUtils.trace(log, "StateTokenDaoImpl: size is now:", map.size());
    }

    public StateToken get(File f) {
        LogUtils.trace(log, "get", f.getAbsolutePath());
        List<StateToken> list = findForFolder(f.getParentFile());
        if (list == null) {
            return null;
        }
        String path = f.getAbsolutePath();
        for (StateToken t : list) {
            if (t.filePath.equals(path)) {
                return t;
            }
        }
        return null;
    }

    public void softDelete(StateToken token) {
        File f = new File(token.filePath);
        List<StateToken> list = findForFolder(f.getParentFile());
        if (list == null) {
            return;
        }
        Iterator<StateToken> it = list.iterator();
        while (it.hasNext()) {
            StateToken t = it.next();
            if (t.filePath.equals(token.filePath)) {
                t.currentCrc = null;
            }
        }
    }    
    
    public void delete(StateToken token) {
        File f = new File(token.filePath);
        List<StateToken> list = findForFolder(f.getParentFile());
        if (list == null) {
            return;
        }
        Iterator<StateToken> it = list.iterator();
        while (it.hasNext()) {
            StateToken t = it.next();
            if (t.filePath.equals(token.filePath)) {
                it.remove();
            }
        }
    }
}
