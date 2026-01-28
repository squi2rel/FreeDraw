package com.github.squi2rel.freedraw.render;

import java.util.ArrayDeque;
import java.util.Deque;

public class BufferQueue {
    private static final Deque<DirtyBuffer> UPLOAD_QUEUE = new ArrayDeque<>();
    private static final int MAX = 8192;

    public static void markDirty(DirtyBuffer buf) {
        if (!UPLOAD_QUEUE.contains(buf)) {
            UPLOAD_QUEUE.add(buf);
        }
    }

    public static void processQueue() {
        int processedNodes = 0;

        while (!UPLOAD_QUEUE.isEmpty() && processedNodes < MAX) {
            DirtyBuffer buf = UPLOAD_QUEUE.poll();
            if (buf == null || !buf.dirty) continue;

            processedNodes += buf.getPoints();
            buf.rebuild();
        }
    }

    public static void clear() {
        UPLOAD_QUEUE.clear();
    }
}