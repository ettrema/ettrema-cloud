package com.ettrema.backup.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides access to a database of tokens (ie CRC's) for local files
 *
 * @author brad
 */
public class StateTokenDaoImpl {

    // todo: replace with db
    private Map<File, StateToken> mapOfTokens = new ConcurrentHashMap<File, StateToken>();
    private Map<File, Map<String, StateToken>> mapOfLists = new ConcurrentHashMap<File, Map<String, StateToken>>();

    public StateToken get(File file) {
        return mapOfTokens.get(file);
    }

    public void set(File file, long crc, long time) {
        System.out.println("StateTokenDaoImpl.set --- " + file.getAbsolutePath() + " - crc: " + crc);
        StateToken st = new StateToken(file.getAbsolutePath(), crc, time);
        mapOfTokens.put(file, st);
        File parent = file.getParentFile();
        Map<String, StateToken> list = mapOfLists.get(parent);
        if (list == null) {
            list = new ConcurrentHashMap<String, StateToken>();
            mapOfLists.put(parent, list);
        }
        list.put(file.getName(), st);
    }

    /**
     * Return all tokens directly inside the given directory
     * 
     * @param scanDir
     * @return 
     */
    public List<StateToken> findForParent(File scanDir) {
        List<StateToken> list = new ArrayList<StateToken>();
        Map<String, StateToken> map = mapOfLists.get(scanDir);
        if (map != null) {
            Collection<StateToken> tokens = map.values();
            list.addAll(tokens);
        }
        return list;
    }

    public void remove(File file) {
        System.out.println("remove-------------------");
        mapOfTokens.remove(file.getAbsoluteFile());
        File parent = file.getParentFile();
        Map<String, StateToken> list = mapOfLists.get(parent);
        if (list == null) {
            list = new ConcurrentHashMap<String, StateToken>();
            mapOfLists.put(parent, list);
        }
        list.remove(file.getName());
    }
}
