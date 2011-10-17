package com.firstclassmandarin.github;

import com.github.api.v2.services.CommitService;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.ObjectService;
import com.github.api.v2.services.RepositoryService;

public class GitHubServices {

    private static final GitHubServiceFactory factory = GitHubServiceFactory.newInstance();

    private GitHubServices() {
    }

    public static RepositoryService getRepositoryService() {
        return factory.createRepositoryService();
    }

    public static ObjectService getObjectService() {
        return factory.createObjectService();
    }

    public static CommitService getCommitService() {
        return factory.createCommitService();
    }
}
