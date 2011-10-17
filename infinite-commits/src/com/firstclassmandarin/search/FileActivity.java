package com.firstclassmandarin.search;

import static com.firstclassmandarin.github.CommitLoader.PATH_DELIMITER;

import java.util.ArrayList;
import java.util.List;

import com.firstclassmandarin.graph.CommitEntry;

public class FileActivity implements Comparable<FileActivity> {

    private final String name;
    private final String hash;

    private String shortName;
    private final List<CommitEntry> commits = new ArrayList<CommitEntry>();
    private long commitTimeSpan = 0;

    public FileActivity(String name, String hash) {
        this.name = name;
        this.hash = hash;
        shortName = name;
        shortName = shortName.substring(shortName.indexOf(PATH_DELIMITER) + 1); // strip root
        shortName = shortName.substring(shortName.indexOf(PATH_DELIMITER) + 1); // strip user
        shortName = shortName.substring(shortName.indexOf(PATH_DELIMITER) + 1); // strip repo
        shortName = shortName.substring(shortName.indexOf(PATH_DELIMITER) + 1); // strip branch
    }

    public void addCommit(CommitEntry commit, long timeUntilNextCommit) {
        commits.add(commit);
        commitTimeSpan += timeUntilNextCommit;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public String getShortName() {
        return shortName;
    }

    public List<CommitEntry> getCommits() {
        return commits;
    }

    @Override
    public String toString() {
        return "[" + name + "] had " + getCommits().size() + " commits over a span of " + getCommitTimeSpanMinutes() + " minutes";
    }

    @Override
    public int compareTo(FileActivity other) {
        // want DESCending order based on commit count
        int sizeDiff = other.commits.size() - commits.size();
        if (sizeDiff != 0)
            return sizeDiff;

        // want ASCending order based on commit time span
        long timeSpanDiff = getCommitTimeSpanMinutes() - other.getCommitTimeSpanMinutes();
        if (timeSpanDiff != 0)
            return (int) timeSpanDiff;

        // otherwise sort by name (path)
        return name.compareTo(other.name);
    }

    public long getCommitTimeSpanMinutes() {
        return commitTimeSpan / 1000 / 60;
    }
}
