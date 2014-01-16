package org.elasticsearch.rest.action.keywordExtraction.keywordRanking;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.client.Client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: michael
 * Date: 1/8/14
 * Time: 11:47 AM
 */
public class TFIDFRank implements KeywordCalculation {
    @Override
    public List<KeyWordScore> getKeyWords( TermVectorResponse termVectorResponse, String fieldName, Client client ) throws IOException {
        ArrayList<KeyWordScore> returningKeyWordScores = new ArrayList<KeyWordScore>();
        Terms content = null;
        String term;
        double score;
        content = termVectorResponse.getFields().terms( fieldName );
        TermsEnum iterator = content.iterator( TermsEnum.EMPTY );
        while ( iterator.next() != null ){
            term = iterator.term().utf8ToString();
            score = basicTFIDF( iterator.docsAndPositions( null, null ).freq(), iterator.docFreq(), -1, content.getDocCount() );
            returningKeyWordScores.add( new KeyWordScore( score, term ) );
        }

        return returningKeyWordScores;
    }


    double basicTFIDF(int frequency, int docFrequency, int overAllFrequency, int totalDocumentCount){
        //log frequency
        return (double) (frequency *
                //inverted document frequency
                Math.log(1.0 *totalDocumentCount / docFrequency));
    }
}
