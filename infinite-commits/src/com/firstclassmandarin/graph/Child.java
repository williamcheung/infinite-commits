package com.firstclassmandarin.graph;

import com.infinitegraph.BaseEdge;

public class Child extends BaseEdge {

    public static enum Type {
        FOLDER,
        FILE
    }

    private String type;

    public Child(Type type) {
        setType(type);
    }

    public Type getType() {
        fetch();
        return Type.valueOf(type);
    }

    public void setType(Type type) {
        markModified();
        this.type = type.name();
    }

    @Override
    public String toString() {
        fetch();
        return type;
    }
}
