package org.elasticsearch.rest.action.keywordExtraction.keywordRanking;

import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;
import java.util.List;

/**
 * User: michael
 * Date: 1/6/14
 * Time: 12:30 PM
 */
public interface KeywordCalculation {

    public List<KeyWordScore> getKeyWords( TermVectorResponse termVectorResponse, String fieldName, Client client ) throws IOException;

}
