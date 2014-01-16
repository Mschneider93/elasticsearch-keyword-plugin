package org.elasticsearch.plugin.keywordExtraction;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.keywordExtraction.KeywordExtractionAction;

public class KeywordExtractionPlugin extends AbstractPlugin {

    public String name() {
        return "Jimdo's KeywordExtraction Plugin";
    }

    public String description() {
        return "Jimdo's KeywordExtraction Plugin";
    }

    @Override public void processModule(Module module) {
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(KeywordExtractionAction.class);
        }
    }
}
