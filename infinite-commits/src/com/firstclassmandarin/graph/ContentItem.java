package com.firstclassmandarin.graph;

import com.firstclassmandarin.github.CommitLoader;
import com.infinitegraph.BaseVertex;

public abstract class ContentItem extends BaseVertex {

    private String name;
    private String hash;
    private String shortName;

    public ContentItem(String name, String hash) {
        setName(name);
        setHash(hash);
    }

    public String getName() {
        fetch();
        return name;
    }

    public void setName(String name) {
        markModified();
        this.name = name;
        this.shortName = name.substring(name.lastIndexOf(CommitLoader.PATH_DELIMITER) + 1);
    }

    public String getHash() {
        fetch();
        return hash;
    }

    public void setHash(String hash) {
        markModified();
        this.hash = hash;
    }

    public String getShortName() {
        fetch();
        return shortName;
    }

    @Override
    public String toString() {
        fetch();
        return name;
    }
}
