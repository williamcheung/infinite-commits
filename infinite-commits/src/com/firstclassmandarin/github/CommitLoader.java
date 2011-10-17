package com.firstclassmandarin.github;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.api.v2.schema.Commit;
import com.github.api.v2.schema.Delta;
import com.github.api.v2.services.CommitService;
import com.github.api.v2.services.GitHubException;


public class CommitLoader {

    public static final String PATH_DELIMITER = "/";
    public static final String SESSION_HASH = "session";
    public static final String USER_HASH = "user";
    public static final String REPOSITORY_HASH = "repo";
    public static final String BRANCH_HASH = "branch";
    private static final String FILE_HASH = "file";
    private static final String FOLDER_HASH = "folder";

    public interface LoadListener {

        void notifyFolder(String name, String hash);

        void notifyFile(String filePath, String hash, Commit commit);
    }

    private static final Logger logger = LoggerFactory.getLogger(CommitLoader.class);

    private final LoadListener listener;
    private final ApiRateLimiter rateLimiter = ApiRateLimiter.getInstance();

    public CommitLoader(LoadListener listener) {
        this.listener = listener;
    }

    public boolean loadCommits(String sessionId, String userName, String repositoryName, String branchName, long maxTime) {
        logger.info("GET {} on https://github.com/{}/{}", new Object[] {branchName, userName, repositoryName});

        String virtualFolder = PATH_DELIMITER + sessionId;
        listener.notifyFolder(virtualFolder, SESSION_HASH);

        virtualFolder = virtualFolder + PATH_DELIMITER + userName;
        listener.notifyFolder(virtualFolder, USER_HASH);

        virtualFolder = virtualFolder + PATH_DELIMITER + repositoryName;
        listener.notifyFolder(virtualFolder, REPOSITORY_HASH);

        virtualFolder = virtualFolder + PATH_DELIMITER + branchName;
        listener.notifyFolder(virtualFolder, BRANCH_HASH);

        return navigateTree(userName, repositoryName, branchName, maxTime, virtualFolder);
    }

    private boolean navigateTree(String userName, String repositoryName, String branchName, long maxTime, String virtualFolder) {
        long startTime = System.currentTimeMillis();

        CommitService commitService = GitHubServices.getCommitService();

        int pageNumber = 1;
        while (true) {
            try {
                rateLimiter.preCall();
                List<Commit> commits = commitService.getCommits(userName, repositoryName, branchName, pageNumber++);
                rateLimiter.postCall();

                if (commits.isEmpty())
                    break;

                for (Commit commitSummary : commits) { // commits returned newest first
                    rateLimiter.preCall();
                    Commit commit = commitService.getCommit(userName, repositoryName, commitSummary.getId());
                    rateLimiter.postCall();

                    List<String> adds = commit.getAdded();
                    if (adds != null) {
                        for (String add: adds) {
                            processFile(virtualFolder, add, commit);
                        }
                    }
                    List<Delta> modifies = commit.getModified();
                    if (modifies != null) {
                        for (Delta modify : modifies) {
                            processFile(virtualFolder, modify.getFilename(), commit);
                        }
                    }
                }

                if (System.currentTimeMillis() - startTime > maxTime) {
                    logger.info("GitHub queries exceeded user patience level of " + (maxTime / 1000 / 60) + " minutes. Stopping this search.");
                    return false;
                }
            } catch (GitHubException e) {
                if (e.getLocalizedMessage().toUpperCase().contains("Not Found".toUpperCase())) // EOL
                    return true;
                throw e;
            }
        }

        return true;
    }

    private void processFile(String virtualFolder, String file, Commit commit) {
        String pathElements[] = file.split(PATH_DELIMITER);
        String previousFolder = "";
        for (int i = 0; i < pathElements.length - 1; i++) {
            String currentFolder = previousFolder + PATH_DELIMITER + pathElements[i];

            String folderPath = virtualFolder + currentFolder;
            listener.notifyFolder(folderPath, FOLDER_HASH);

            previousFolder = currentFolder;
        }

        String filePath = virtualFolder + PATH_DELIMITER + file;
        listener.notifyFile(filePath, FILE_HASH, commit);
    }
}
