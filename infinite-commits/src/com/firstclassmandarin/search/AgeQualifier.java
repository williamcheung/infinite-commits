package com.firstclassmandarin.search;

import java.util.Date;

import com.firstclassmandarin.graph.CommitEntry;
import com.firstclassmandarin.graph.File;
import com.firstclassmandarin.graph.Folder;
import com.infinitegraph.Vertex;
import com.infinitegraph.navigation.Path;
import com.infinitegraph.navigation.Qualifier;

public class AgeQualifier implements Qualifier {

    private final long maxAgeMilliseconds;

    public AgeQualifier(int spanInDays) {
        this.maxAgeMilliseconds = ((long)spanInDays)*24/*hrs/day*/*60/*min/hr*/*60/*sec/min*/*1000/*millsec/sec*/;
    }

    @Override
    public boolean qualify(Path path) {
        Vertex vertex = path.getFinalHop().getVertex();
        if (vertex instanceof Folder || vertex instanceof File)
            return true;

        if (vertex instanceof CommitEntry) {
            CommitEntry commit = (CommitEntry) vertex;
            return qualify(commit);
        }

        throw new RuntimeException("Unexpected vertex type: " + vertex.getClass());
    }

    public boolean qualify(CommitEntry commit) {
        long commitTime = commit.getCommitDate().getTime();
        long currentTime = new Date().getTime();

        long commitAge = currentTime - commitTime;
        if (commitAge <= 0)
            throw new RuntimeException("Unexpected commit date for: {}" + commit);

        boolean tooOld = commitAge > maxAgeMilliseconds;
        return !tooOld;
    }
}
