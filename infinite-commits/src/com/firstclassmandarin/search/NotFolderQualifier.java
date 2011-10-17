package com.firstclassmandarin.search;

import com.firstclassmandarin.graph.Folder;
import com.infinitegraph.navigation.Path;
import com.infinitegraph.navigation.Qualifier;

public class NotFolderQualifier implements Qualifier {

    @Override
    public boolean qualify(Path path) {
        return !(path.getFinalHop().getVertex() instanceof Folder);
    }
}
