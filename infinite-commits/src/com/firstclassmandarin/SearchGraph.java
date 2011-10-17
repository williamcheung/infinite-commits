package com.firstclassmandarin;

import java.io.File;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.firstclassmandarin.search.FileActivity;
import com.firstclassmandarin.search.GraphSearcher;
import com.infinitegraph.AccessMode;
import com.infinitegraph.ConfigurationException;
import com.infinitegraph.GraphDatabase;
import com.infinitegraph.GraphFactory;
import com.infinitegraph.StorageException;
import com.infinitegraph.Transaction;
import com.objy.db.app.Connection;

public class SearchGraph implements AppConstants {

    private static final Logger logger = LoggerFactory.getLogger(SearchGraph.class);
    private static final String PROPERTIES_FILE_NAME = "SearchGraph.properties";

    public static void main(String[] args)
    {
        int i = 0;
        String sessionId = args[i++];
        String user = args[i++];
        String repo = args[i++];
        String branch = args[i++];
        int spanInDays = Integer.parseInt(args[i++]);

        SortedSet<FileActivity> fileActivities = new SearchGraph(".").findMostActiveFiles(sessionId, user, repo, branch, spanInDays);
        i = 1;
        for (FileActivity fileActivity : fileActivities) {
            logger.info("{}. {}", i++, fileActivity.toString());
        }
    }

    private final String propertiesFile;

    public SearchGraph(String propertiesDir) {
        propertiesFile = new File(propertiesDir, PROPERTIES_FILE_NAME).getAbsolutePath();
    }

    public SortedSet<FileActivity> findMostActiveFiles(String sessionId, String user, String repo, String branch, int spanInDays) {
        // Handles for transaction and graph database instances
        Transaction tx = null;
        GraphDatabase graphDB = null;

        try
        {
            // Open the graph database instance here.
            graphDB = GraphFactory.open(GRAPH_DB_NAME, propertiesFile);

            Connection.current().useContextClassLoader(true);

            // Begin a read transaction
            logger.info("> Starting a read transaction ...");
            tx = graphDB.beginTransaction(AccessMode.READ);

            // Search graph.
            GraphSearcher search = new GraphSearcher(graphDB, sessionId, user, repo, branch, spanInDays);
            SortedSet<FileActivity> fileActivities = search.findMostActiveFiles();
            logger.info("> Search completed ...");
            return fileActivities;
        }
        catch (ConfigurationException cE)
        {
            logger.info("> CONFIGURATION EXCEPTION: " + cE.getMessage());
            throw new RuntimeException(cE);
        }
        catch(StorageException sE)
        {
            logger.info("> STORAGE EXCEPTION: " + sE.getMessage());
            throw new RuntimeException(sE);
        }
        finally
        {
            {
                try
                {
                    // Complete the transaction.
                    if (tx != null)
                        tx.complete();

                    // Close the graph database.
                    if (graphDB != null)
                        graphDB.close();
               }
               catch(StorageException sE)
               {
                   logger.info("> STORAGE EXCEPTION: " + sE.getMessage());
               }
            }
        }
    }
}
