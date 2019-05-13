package com.igor_shaula.location_tracker.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.location_tracker.entity.LocationPoint;

import java.util.List;

/**
 * Created by igor shaula
 * <p/>
 * this interface is used to be the only agent of storage system \
 */
public interface StorageActions {

    boolean write(@Nullable LocationPoint locationPoint);

    @NonNull
    List<LocationPoint> readAll();

    void clearAll();
}