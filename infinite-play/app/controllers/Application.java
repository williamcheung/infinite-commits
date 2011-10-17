package controllers;

import java.io.File;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.mvc.Controller;

import com.firstclassmandarin.GraphManager;
import com.firstclassmandarin.SearchGraph;
import com.firstclassmandarin.search.FileActivity;

public class Application extends Controller {

    private static final String CONF_DIR = getConfDir();
    private static final GraphManager manager = new GraphManager(CONF_DIR);
    private static final SearchGraph searcher = new SearchGraph(CONF_DIR);

    private static final String USER_KEY = "user";
    private static final String REPO_KEY = "repo";
    private static final String BRANCH_KEY = "branch";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String SPAN_IN_DAYS_KEY = "spanInDays";
    private static final String PATIENCE_MINUTES_KEY = "patienceMinutes";
    private static final String NEW_SESSION_KEY = "newSession";

    private static final int DEFAULT_SPAN_IN_DAYS = 7;
    private static final int DEFAULT_PATIENCE_MINUTES = 2;

    @OnApplicationStart
    public class Bootstrap extends Job {

        @Override
        public void doJob() throws Exception {
            manager.createGraph();
        }
    }

    public static void index() {
        render();
    }

    public static void loadGraph(String user, String repo, String branch, int spanInDays, int patienceMinutes, boolean newSession) {
        if (spanInDays <= 0)
            spanInDays = DEFAULT_SPAN_IN_DAYS;

        if (patienceMinutes <= 0)
            patienceMinutes = DEFAULT_PATIENCE_MINUTES;

        if (newSession)
            session.put(NEW_SESSION_KEY, "true");
        else
            session.remove(NEW_SESSION_KEY);

        String oldUser = session.get(USER_KEY);
        String oldRepo = session.get(REPO_KEY);
        String oldBranch = session.get(BRANCH_KEY);
        String sessionId = session.get(SESSION_ID_KEY);

        int oldSpanInDays = DEFAULT_SPAN_IN_DAYS;
        try {
            oldSpanInDays = Integer.parseInt(session.get(SPAN_IN_DAYS_KEY));
        } catch (NumberFormatException e) {
        }

        int oldPatienceMinutes = DEFAULT_PATIENCE_MINUTES;
        try {
            oldPatienceMinutes = Integer.parseInt(session.get(PATIENCE_MINUTES_KEY));
        } catch (NumberFormatException e) {
        }

        boolean oldSessionStillValid =
            user.equals(oldUser) && repo.equals(oldRepo) && branch.equals(oldBranch) &&
            spanInDays == oldSpanInDays &&
            patienceMinutes == oldPatienceMinutes &&
            sessionId != null;

        AtomicBoolean cancelled = new AtomicBoolean();

        if (!oldSessionStillValid || newSession) {
            session.put(USER_KEY, user);
            session.put(REPO_KEY, repo);
            session.put(BRANCH_KEY, branch);
            session.put(SPAN_IN_DAYS_KEY, spanInDays);
            session.put(PATIENCE_MINUTES_KEY, patienceMinutes);

            sessionId = startNewSession(user, repo, branch, patienceMinutes, cancelled);
        }

        SortedSet<FileActivity> fileActivities;
        try {
            fileActivities = searcher.findMostActiveFiles(sessionId, user, repo, branch, spanInDays);
        } catch (NullPointerException npe) {
            // session probably stale, start a new one:
            sessionId = startNewSession(user, repo, branch, patienceMinutes, cancelled);
            fileActivities = searcher.findMostActiveFiles(sessionId, user, repo, branch, spanInDays);
        }

        boolean queryCancelled = cancelled.get();
        render(user, repo, branch, spanInDays, fileActivities, queryCancelled);
    }

    private static String startNewSession(String user, String repo, String branch, int patienceMinutes,
            AtomicBoolean cancelled) {
        long maxTime = patienceMinutes * 60 * 1000;
        String sessionId = manager.loadGraph(user, repo, branch, maxTime, cancelled);
        session.put(SESSION_ID_KEY, sessionId);
        return sessionId;
    }

    private static String getConfDir() {
        return new File(Play.applicationPath, "conf").getAbsolutePath();
    }
}