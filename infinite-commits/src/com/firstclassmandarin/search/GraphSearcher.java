package com.firstclassmandarin.search;

import static com.firstclassmandarin.github.CommitLoader.PATH_DELIMITER;

import java.util.SortedSet;
import java.util.TreeSet;

import com.firstclassmandarin.graph.CommitEntry;
import com.firstclassmandarin.graph.File;
import com.firstclassmandarin.graph.Previous;
import com.infinitegraph.Edge;
import com.infinitegraph.GraphDatabase;
import com.infinitegraph.Vertex;
import com.infinitegraph.navigation.Guide;
import com.infinitegraph.navigation.Hop;
import com.infinitegraph.navigation.NavigationResultHandler;
import com.infinitegraph.navigation.Navigator;
import com.infinitegraph.navigation.Path;
import com.infinitegraph.navigation.Qualifier;

public class GraphSearcher {

    private class MostActiveFilesListBuilder implements NavigationResultHandler {
        private final SortedSet<FileActivity> files = new TreeSet<FileActivity>();
        private FileActivity currentFile;
        private final AgeQualifier ageQualifier;

        public MostActiveFilesListBuilder(AgeQualifier ageQualifier) {
            this.ageQualifier = ageQualifier;
        }

        @Override
        public void handleResultPath(Path path, Navigator navigator) {
            Hop lastHop = path.getFinalHop();
            Vertex vertex = lastHop.getVertex();
            if (vertex instanceof File) {
                if (currentFile != null)
                    files.add(currentFile);

                File file = (File) vertex;
                String fileName = file.getName();
                fileName = fileName.replace(PATH_DELIMITER + sessionId, "");

                currentFile = new FileActivity(fileName, file.getHash());
            } else if (vertex instanceof CommitEntry) {
                CommitEntry commit = (CommitEntry) vertex;
                if (ageQualifier.qualify(commit)) {
                    long timeUntilNextCommit = 0;
                    Edge edge = lastHop.getEdge();
                    if (edge instanceof Previous) {
                        Previous previous = (Previous) edge;
                        timeUntilNextCommit = previous.getTimeInterval();
                    }

                    currentFile.addCommit(commit, timeUntilNextCommit);
                } else if (currentFile.getCommits().isEmpty()) {
                    files.remove(currentFile);
                    currentFile = null;
                }
            } else
                throw new RuntimeException("Unexpected vertex type: " + vertex.getClass());
        }

        public SortedSet<FileActivity> getMostActiveFiles() {
            if (currentFile != null) {
                files.add(currentFile);
                currentFile = null;
            }

            return files;
        }
    }

    private final GraphDatabase graphDB;
    private final String sessionId;
    private final String startingSearchVertexName;

    private int spanInDays = 7;

    public GraphSearcher(GraphDatabase graphDB, String sessionId, String userName, String repositoryName, String branchName, Integer spanInDays) {
        this.graphDB = graphDB;
        this.sessionId = sessionId;
        startingSearchVertexName = PATH_DELIMITER + sessionId + PATH_DELIMITER + userName + PATH_DELIMITER + repositoryName + PATH_DELIMITER + branchName;
        if (spanInDays != null)
            this.spanInDays = spanInDays;
    }

    public SortedSet<FileActivity> findMostActiveFiles() {
        Guide guide = Guide.SIMPLE_DEPTH_FIRST;
        AgeQualifier pathQualifier = new AgeQualifier(spanInDays);
        Qualifier resultQualifier = new NotFolderQualifier();

        MostActiveFilesListBuilder resultHandler = new MostActiveFilesListBuilder(pathQualifier);

        Vertex startVertex = graphDB.getNamedVertex(startingSearchVertexName);
        Navigator nav = startVertex.navigate(guide, pathQualifier, resultQualifier, resultHandler);
        nav.start();
        nav.stop();
        return resultHandler.getMostActiveFiles();
    }

    public int getSpanInDays() {
        return spanInDays;
    }

    public void setSpanInDays(int spanInDays) {
        this.spanInDays = spanInDays;
    }
}
