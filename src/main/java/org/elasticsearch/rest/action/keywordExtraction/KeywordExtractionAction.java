package org.elasticsearch.rest.action.keywordExtraction;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvector.TermVectorRequest;
import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.BaseRestHandler;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.RestStatus.METHOD_NOT_ALLOWED;

import org.elasticsearch.rest.action.keywordExtraction.keywordRanking.KeyWordScore;
import org.elasticsearch.rest.action.keywordExtraction.keywordRanking.KeywordCalculation;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.action.support.RestXContentBuilder.restContentBuilder;

public class KeywordExtractionAction extends BaseRestHandler {
    private static final String PREFIX_RANKING_PACKAGE = "org.elasticsearch.rest.action.keywordExtraction.keywordRanking.";
    public static String KEYWORD_URI  = "_keyword";
    public static String TERMVECTOR_URI= "_termvector";

    public static String PARAM_URL = "url";
    public static String CONTENT = "content";
    public static String[] CONSIDERED_FIELDS = new String[]{ CONTENT };
    public static String PARAM_URL_SEARCH = "urlField";
    public static String DEFAULT_URL_SEARCH_FIELD = "webPage.url";
    public static Boolean TRUE = true;
    public static String PARAM_KEYWORE_CALC_TYPE = "keywordCalculationType";
    public static String PARAM_KEYWORE_SIZE = "keywordSize";
    public static int DEFAULT_MAX_KEYWORD_SIZE = 15;

    /**
     * Initializing keyword-rest-service
     *
     * @param settings
     * @param client
     * @param controller
     */
     @Inject
    public KeywordExtractionAction( Settings settings, Client client, RestController controller ) {
        super( settings, client );

        // Define REST endpoints
        controller.registerHandler( GET, "/{index}/{type}/_keyword/", this );
        controller.registerHandler( GET, "/{index}/{type}/_termvector/", this );
        controller.registerHandler( POST, "/{index}/{type}/_keyword/", this );
        controller.registerHandler( POST, "/{index}/{type}/_termvector/", this );
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        try {
            String index = request.param("index");
            String type = request.param("type");
            if(request.uri().trim().endsWith( KEYWORD_URI )){
                getKeywords( request, channel, index, type );
            }
            else if ( request.uri().endsWith( TERMVECTOR_URI )) {
                getTermVector( request, channel, index, type  );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /***
     * Handles the request for retrieving a set of keywords based on the website URL
     *
     * 1) first the method searches for an url in order to determine the document id
     * 2) based on the document id the method fetches the termvector
     * 3) performs a ranking based on the request-parameter
     *
     *
     * Example curl
     * curl -XGET 'http://127.0.0.1:9200/paying_customer/webPage/_keyword' -d '{ "url": "http://www.hello.de", "keywordCalculationType":"LogTFIDFRank" }'
     * @param request
     * @param channel
     */
    public void getKeywords(final RestRequest request, final RestChannel channel, String index, String type) {
        logger.debug("KeywordExtraction called");
        System.out.println("KeywordExtraction.handleRequest called");
        /********************Getting Parameter *************/
        KeywordCalculation keyWordCalculation = null;
        String urlQuerry = null;
        String urlSearchField;
        String id;
        int maxKeyWordSize;

        /*******************Getting the url query type***********/
        String requestString = request.content().toUtf8();
        try {
            JsonObject requestObject  = new JsonParser().parse( requestString ).getAsJsonObject();
            // Getting required Parameter, i.e. the url in order to find the id of a given url
            urlQuerry = requestObject.get( PARAM_URL ).getAsString();
            String keywordComputationClass = requestObject.get( PARAM_KEYWORE_CALC_TYPE ).getAsString();
            keyWordCalculation = getKeyWordCalculation( keywordComputationClass );

            //Getting optional Parameter
            urlSearchField = requestObject.has( PARAM_URL_SEARCH ) ? requestObject.get( PARAM_URL_SEARCH ).getAsString() : DEFAULT_URL_SEARCH_FIELD;
            maxKeyWordSize = requestObject.has( PARAM_KEYWORE_SIZE ) ? requestObject.get( PARAM_KEYWORE_SIZE ).getAsInt() : DEFAULT_MAX_KEYWORD_SIZE;
            id = getIDofUrl( urlQuerry, urlSearchField, index, type );
            if(id.equals( "" )){
                sendErrorMessage( "Nothing found for the url : " + urlQuerry + "\nMaybe check if Analyzer for the url field is not destroying the indexed information, incorrect index or type", channel );
                return;
            }

        } catch ( ClassNotFoundException e ) {
            sendErrorMessage( "Couldn't Find keywordComputationClass: " + keyWordCalculation, channel );
            return;
        } catch ( InstantiationException e ) {
            sendErrorMessage( "Couldn't instantiate: " + keyWordCalculation, channel );
            return;
        } catch ( IllegalAccessException e ) {
            sendErrorMessage("Couldn't access: " + keyWordCalculation , channel);
            return;
        }

        if(keyWordCalculation == null){
            sendErrorMessage( "Couyld not initialize keywordComputationClass ", channel );
            return;
        }

        TermVectorResponse termVectorResponse = termVectorRequest( id, index, type );

        /*******************Calculating keywords and their scores*/
        List<KeyWordScore> keyWords = null;
        if ( keyWordCalculation != null ) {
            try {
                keyWords = keyWordCalculation.getKeyWords( termVectorResponse, CONTENT, client );
                Collections.sort( keyWords );
                if( keyWords.size() < maxKeyWordSize) { maxKeyWordSize = keyWords.size(); }
                keyWords = keyWords.subList( 0, maxKeyWordSize );
            } catch ( IOException e ) {
                sendErrorMessage( "Could not initialize keywordComputationClass ", channel );
                return;
            }
        }
        JsonElement element = new Gson().toJsonTree(keyWords, new TypeToken<List<KeyWordScore>>() {}.getType());
        JsonArray returningTerms = element.getAsJsonArray().getAsJsonArray();
        channel.sendResponse( new StringRestResponse( OK, returningTerms.toString() ) );
        return;
    }

    public void getTermVector(final RestRequest request, final RestChannel channel, String index, String type) throws IOException {
        logger.debug("TermVector Information called");
        String requestString = request.content().toUtf8();
        System.out.println("TermVectorinformation ----\n"+requestString);
        JsonObject requestObject  = new JsonParser().parse( requestString ).getAsJsonObject();
        // Getting required Parameter, i.e. the url in order to find the id of a given url
        String urlQuerry = requestObject.get( PARAM_URL ).getAsString();
        // Getting required Parameter, i.e. the url in order to find the id of a given url
        String urlSearchField = requestObject.has( PARAM_URL_SEARCH ) ? requestObject.get( PARAM_URL_SEARCH ).getAsString() : DEFAULT_URL_SEARCH_FIELD;
        TermVectorResponse termVectorResponse = termVectorRequest( urlQuerry, urlSearchField, index, type );
        XContentBuilder builder = restContentBuilder(request);
        termVectorResponse.toXContent(builder, request);
        channel.sendResponse(new XContentRestResponse(request, OK, builder));
    }

    /***
     * Getting the termVector information
     *
     * @param index
     * @param type
     * @param id
     * @return
     */
    private TermVectorResponse termVectorRequest( String id, String index, String type ) {
        TermVectorRequest termVectorRequest = new TermVectorRequest( index, type, id ).selectedFields( CONSIDERED_FIELDS ).termStatistics( TRUE ).fieldStatistics( TRUE );
        return  client.termVector( termVectorRequest ).actionGet();
    }
    
    /***
     * Getting the termVector information
     *
     * @param index
     * @param type
     * @param urlQuerry
     * @param urlSearchField
     * @return
     */
    private TermVectorResponse termVectorRequest( String urlQuerry, String urlSearchField, String index, String type) {
        String id = getIDofUrl( urlQuerry, urlSearchField, index, type );
        return  termVectorRequest( id, index, type );
    }


    /***
     * Method in order to retrieve the right keyword ranking method
     *
     * @param className
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    KeywordCalculation getKeyWordCalculation(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> keywordClass = Class.forName(PREFIX_RANKING_PACKAGE + className);
        if (!KeywordCalculation.class.isAssignableFrom(keywordClass)) {
            throw new IllegalArgumentException();
        }
        return (KeywordCalculation) keywordClass.newInstance();
    }

    /**
     * log error and send error msg to client.
     * @param msg
     * @param channel
     */
    public void sendErrorMessage(String msg, RestChannel channel){
        logger.error( msg, this );
        channel.sendResponse(new StringRestResponse( METHOD_NOT_ALLOWED, msg));
    }


    public String getIDofUrl(String urlQuerry, String urlSearchField, String index, String type){
        /*******************Getting the url query type***********/
        QueryBuilder termQueryBuilder = new TermQueryBuilder( urlSearchField, urlQuerry ); // taking this one, because keywords are determined per site
        SearchResponse searchResponse = client.prepareSearch( index ).setTypes( type ).setQuery( termQueryBuilder ).execute().actionGet();
        System.out.println("hit size : " + searchResponse.getHits().getHits().length);

        /*******************Stop when nothing is found or keyWordCalculation is null***********/
        if( searchResponse.getHits().getHits().length == 0) {
            return "";
        }

        /*******************Getting the the id **************/
        return searchResponse.getHits().iterator().next().getId();
    }

}
