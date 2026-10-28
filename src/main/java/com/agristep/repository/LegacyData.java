package com.agristep.repository;

/**
 * Beds, history and settings saved by the earlier single-garden version have no owner. The first account
 * created takes them over, so nothing someone already planted is lost when they upgrade.
 */
public interface LegacyData {
    boolean exists();

    void assignTo(long userId);
}
