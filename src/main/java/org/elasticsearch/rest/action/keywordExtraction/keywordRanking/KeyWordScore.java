package org.elasticsearch.rest.action.keywordExtraction.keywordRanking;

/**
 * User: michael
 * Date: 1/7/14
 * Time: 12:09 PM
 */
public class KeyWordScore implements Comparable<KeyWordScore> {
    double score;
    String keyword;


    public KeyWordScore(double score, String keyword){
        this.keyword = keyword;
        this.score = score;
    }

    @Override
    public int compareTo( KeyWordScore o ) {
        return score > o.score ? -1 : score > o.score ? 1 : 0;
    }

    public String toString(){
        return keyword + " - " + score;
    }
}
