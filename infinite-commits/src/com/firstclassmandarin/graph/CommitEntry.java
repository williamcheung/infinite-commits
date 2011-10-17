package com.firstclassmandarin.graph;

import java.util.Date;

import com.infinitegraph.BaseVertex;

public class CommitEntry extends BaseVertex {

    private String hash;
    private String message;
    private long commitDate;

    public CommitEntry(String hash, String message, Date commitDate) {
        setHash(hash);
        setMessage(message);
        setCommitDate(commitDate);
    }

    public String getHash() {
        fetch();
        return hash;
    }

    public void setHash(String hash) {
        markModified();
        this.hash = hash;
    }

    public String getMessage() {
        fetch();
        return message;
    }

    public void setMessage(String message) {
        markModified();
        this.message = message;
    }

    public Date getCommitDate() {
        fetch();
        return new Date(commitDate);
    }

    public void setCommitDate(Date commitDate) {
        markModified();
        this.commitDate = commitDate.getTime();
    }

    @Override
    public String toString() {
        fetch();
        return new Date(commitDate) + " - " + message;
    }
}
