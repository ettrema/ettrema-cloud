package com.ettrema.backup.observer;

/**
 * A generic listener interface for observing data items
 *
 * T is the type being observed,and P is the notional parent of the observed item
 *
 * @author brad
 */
public interface Observer<T,P> {
    void onAdded(T t, P parent);

    /**
     *
     * @param t
     * @param parent
     * @param indexOf - the ordinal of the removed item within the parent
     */
    void onRemoved(T t, P parent, Integer indexOf);

    void onUpdated(T t, P parent);
}
