package com.firstclassmandarin.graph;

import com.infinitegraph.BaseEdge;

public class Previous extends BaseEdge {

    private long timeInterval;

    public Previous(long timeInterval) {
        setTimeInterval(timeInterval);
    }

    public long getTimeInterval() {
        fetch();
        return timeInterval;
    }

    public void setTimeInterval(long timeInterval) {
        markModified();
        this.timeInterval = timeInterval;
    }

    @Override
    public String toString() {
        fetch();
        return String.valueOf(timeInterval);
    }
}
