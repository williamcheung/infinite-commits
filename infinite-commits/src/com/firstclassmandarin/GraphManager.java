package com.firstclassmandarin;

// Import all InfiniteGraph packages

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.firstclassmandarin.github.CommitLoader;
import com.firstclassmandarin.graph.CommitLoadListener;
import com.infinitegraph.AccessMode;
import com.infinitegraph.ConfigurationException;
import com.infinitegraph.GraphDatabase;
import com.infinitegraph.GraphFactory;
import com.infinitegraph.StorageException;
import com.infinitegraph.Transaction;
import com.objy.db.app.Connection;

public class GraphManager implements AppConstants
{
    private static final Logger logger = LoggerFactory.getLogger(GraphManager.class);
    private static final String PROPERTIES_FILE_NAME = "CreateGraph.properties";

    public static void main(String[] args)
    {
        String user = args[0];
        String repo = args[1];
        String branch = args[2];
        int maxMinutes = Integer.parseInt(args[3]);

        GraphManager manager = new GraphManager(".");
        manager.createGraph();

        long maxTime = maxMinutes * 60 * 1000;
        AtomicBoolean cancelled = new AtomicBoolean();
        String sessionId = manager.loadGraph(user, repo, branch, maxTime, cancelled);
        logger.info("sessionId: {} cancelled: {}", sessionId, cancelled);
    }

    private final String propertiesFile;

    public GraphManager(String propertiesDir) {
        propertiesFile = new File(propertiesDir, PROPERTIES_FILE_NAME).getAbsolutePath();
    }

    public String loadGraph(String user, String repo, String branch, long maxTime, AtomicBoolean cancelled) {
        cancelled.set(false);

        // Create null transaction, null graph database instance
    	Transaction tx = null;
	    GraphDatabase graphDB = null;
        String sessionId = UUID.randomUUID().toString();

    	try
        {
			// Open graph database
			logger.info("> Opening graph database {} ...", GRAPH_DB_NAME);
			graphDB = GraphFactory.open(GRAPH_DB_NAME, propertiesFile);

			Connection.current().useContextClassLoader(true);

			// Begin transaction
			logger.info("> Starting a read/write transaction ...");
			tx = graphDB.beginTransaction(AccessMode.READ_WRITE);

			// load commits
	        CommitLoader commitLoader = new CommitLoader(new CommitLoadListener(graphDB));
	        boolean notCancelled = commitLoader.loadCommits(sessionId, user, repo, branch, maxTime);
	        cancelled.set(!notCancelled);

			// Commit to save your changes to the graph database
			logger.info("> Committing changes ...");
			tx.commit();
        }
        catch (ConfigurationException cE)
        {
            logger.warn("> Configuration Exception was thrown ... ");
            logger.error(cE.getMessage());
        }
        finally
        {
            // If the transaction was not committed, complete
            // will roll it back
            if (tx != null)
                tx.complete();
            if (graphDB != null)
            {
                graphDB.close();
                logger.info("> On Exit: Closed graph database {}", GRAPH_DB_NAME);
            }
        }

        return sessionId;
    }

    public void createGraph() {
        logger.info("> Creating graph database {} ...", GRAPH_DB_NAME);
        try
        {
            // Create graph database
            GraphFactory.create(GRAPH_DB_NAME, propertiesFile);
        }
        catch (StorageException sE)
        {
            logger.warn(sE.getMessage());
        }
        catch (ConfigurationException cE)
        {
            logger.warn("> Configuration Exception was thrown ... ");
            logger.error(cE.getMessage());
        }
    }

    public void deleteGraph() {
        logger.info("> Deleting graph database {} ...", GRAPH_DB_NAME);
        try
        {
            // Delete graph database if it already exists
            GraphFactory.delete(GRAPH_DB_NAME, propertiesFile);
        }
        catch (StorageException sE)
        {
            logger.info(sE.getMessage());
        }
        catch (ConfigurationException cE)
        {
            logger.warn("> Configuration Exception was thrown ... ");
            logger.error(cE.getMessage());
        }
    }
}
