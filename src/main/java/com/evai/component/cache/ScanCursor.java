package com.evai.component.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;


@Getter
@Setter
public class ScanCursor<T> {

    public static final ScanCursor<String> EMPTY = new ScanCursor<>();

    private long cursorId;
    private Collection<T> items;
}