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
 * Date: 1/7/14
 * Time: 9:30 AM
 *
 * tf–idf is the product of two statistics, term frequency and inverse document frequency.
 * Various ways for determining the exact values of both statistics exist. In the case of the term
 * frequency tf(t,d), the simplest choice is to use the raw frequency of a term in a document, i.e.
 * the number of times that term t occurs in document d.
 *
 * The inverse document frequency is a measure of whether the term is common or rare across all documents.
 * It is obtained by dividing the total number of documents by the number of documents containing the term,
 * and then taking the logarithm of that quotient.
 *
 */
public class LogTFIDFRank implements KeywordCalculation {
    @Override
    public List<KeyWordScore> getKeyWords( TermVectorResponse termVectorResponse, String fieldName, Client client) throws IOException {
        ArrayList<KeyWordScore> returningKeyWordScores = new ArrayList<KeyWordScore>();
        Terms content = null;
        String term;
        double score;
        content = termVectorResponse.getFields().terms( fieldName );
        TermsEnum iterator = content.iterator( TermsEnum.EMPTY );
        while ( iterator.next() != null ){
            term = iterator.term().utf8ToString();
            score = basicLogTFIDF( iterator.docsAndPositions( null, null ).freq(), iterator.docFreq(), -1, content.getDocCount() );
            returningKeyWordScores.add( new KeyWordScore( score, term ) );
        }

        return returningKeyWordScores;
    }



    double basicLogTFIDF(int frequency, int docFrequency, int overAllFrequency, int totalDocumentCount){
        //log frequency
        double outcome =  Math.log((1+frequency) *
                //inverted document frequency
                Math.log( 1.0*totalDocumentCount / docFrequency));
        return outcome;
    }
}
