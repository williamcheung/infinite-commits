package com.firstclassmandarin.graph;

import static com.firstclassmandarin.github.CommitLoader.PATH_DELIMITER;
import static com.firstclassmandarin.graph.Child.Type.FILE;
import static com.firstclassmandarin.graph.Child.Type.FOLDER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.firstclassmandarin.github.CommitLoader.LoadListener;
import com.firstclassmandarin.graph.Child.Type;
import com.github.api.v2.schema.Commit;
import com.infinitegraph.Edge;
import com.infinitegraph.EdgeHandle;
import com.infinitegraph.EdgeKind;
import com.infinitegraph.GraphDatabase;
import com.infinitegraph.Vertex;

public class CommitLoadListener implements LoadListener {

    private static final Logger logger = LoggerFactory.getLogger(CommitLoadListener.class);

    private final GraphDatabase graph;

    public CommitLoadListener(GraphDatabase graph) {
        this.graph = graph;
    }

    @Override
    public void notifyFolder(String name, String hash) {
        addToGraph(FOLDER, "*Folder*", new Folder(name, hash));
    }

    @Override
    public void notifyFile(String filePath, String hash, Commit commit) {
        addToGraph(FILE, "File", new File(filePath, ""), commit);
    }

    private Vertex addToGraph(Type type, String typeLabel, ContentItem contentItem) {
        String name = contentItem.getName();

        Vertex vertex = graph.getNamedVertex(name);
        if (vertex == null) {
            String hash = contentItem.getHash();
            logger.info("{} [{}] {}", new Object[] {typeLabel, name, hash});
            graph.addVertex(contentItem);
            graph.nameVertex(name, contentItem);

            String pathElements[] = name.split(PATH_DELIMITER);
            if (pathElements.length > 2) {
                String parentName = "";
                for (int i = 0; i < pathElements.length - 1; i++) {
                    String pathElement = pathElements[i];
                    if (!pathElement.isEmpty())
                        parentName = parentName.concat(PATH_DELIMITER).concat(pathElement);
                }
                Vertex parent = graph.getNamedVertex(parentName);
                Child child = new Child(type);
                parent.addEdge(child, contentItem, EdgeKind.OUTGOING);
            }
        }

        return vertex == null ? graph.getNamedVertex(name) : vertex;
    }

    private void addToGraph(Type type, String typeLabel, File file, Commit commit) {
        file = (File) addToGraph(type, typeLabel, file);

        CommitEntry lastCommit = getLastCommit(file);

        CommitEntry currentCommit = new CommitEntry(commit.getId(), commit.getMessage(), commit.getCommittedDate());
        if (lastCommit == null)
            addToGraphWithParent(currentCommit, file);
        else
            addToGraphWithParent(currentCommit, lastCommit);
    }

    private CommitEntry getLastCommit(Vertex vertex) {
        Iterable<EdgeHandle> edges = vertex.getEdges();
        for (EdgeHandle edgeHandle : edges) {
            if (edgeHandle.getKind() == EdgeKind.OUTGOING) {
                Edge edge = edgeHandle.getEdge();
                Vertex candidate = edge.getTarget().getVertex();
                return getLastCommit(candidate);
            }
        }
        return vertex instanceof CommitEntry ? (CommitEntry) vertex : null;
    }

    private void addToGraphWithParent(CommitEntry commit, File parent) {
        addToGraph(commit);
        parent.addEdge(new Latest(), commit, EdgeKind.OUTGOING);
    }

    private void addToGraphWithParent(CommitEntry commit, CommitEntry parent) {
        addToGraph(commit);
        long timeInterval = parent.getCommitDate().getTime() - commit.getCommitDate().getTime();
        parent.addEdge(new Previous(timeInterval), commit, EdgeKind.OUTGOING);
    }

    private void addToGraph(CommitEntry commit) {
        logger.info("COMMIT {} {} [{}]", new Object[] {commit.getHash(), commit.getCommitDate(), commit.getMessage()});
        graph.addVertex(commit);
        graph.nameVertex(commit.getHash(), commit);
    }
}
