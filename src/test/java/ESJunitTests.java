import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.termvector.TermVectorRequest;
import org.elasticsearch.action.termvector.TermVectorResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * User: michael
 * Date: 1/8/14
 * Time: 2:17 PM
 */
public class ESJunitTests {
     public String clusterConf = "cluster.name";
     public String clusterName = "msc_content_container";
     public String nodeClient = "node.client";
     public String url = "localhost";
     public int port = 9300;

    Settings settings = ImmutableSettings.settingsBuilder().put( clusterConf, clusterName ).put( nodeClient , true ).build();
    TransportClient client = new TransportClient(settings).addTransportAddress( new InetSocketTransportAddress( url, port ) );


    @Test
    public void testUrlSearch(){
        QueryBuilder prefixQueryBuilder = new TermQueryBuilder( "webPage.url", "http://www.hello.de" );
        SearchResponse searchResponse = client.prepareSearch( "paying_customer" ).setTypes( "webPage" ).setQuery( prefixQueryBuilder ).execute().actionGet();
        System.out.println("hit size : " + searchResponse.getHits().getHits().length);

        assertTrue(searchResponse.getHits().getHits().length == 1);
    }

    @Test
    public void testTermVector(){
        ActionFuture<TermVectorResponse> termVector = client.termVector(
                new TermVectorRequest( "paying_customer", "webPage", "3" ).selectedFields( new String[]{"content"} ).termStatistics( true ).fieldStatistics( true ) );

        TermVectorResponse termVectorResponse = termVector.actionGet();

        try {
            Terms content = termVectorResponse.getFields().terms( "content" );
            TermsEnum iterator = content.iterator( TermsEnum.EMPTY );
            BytesRef next;
            while (iterator.next() != null ){
                System.out.println("term: " + iterator.term().utf8ToString()
                        + "\t totalTermFreq: " + iterator.totalTermFreq()
                        + "\t docFreq: " +iterator.docFreq()
                        + "\t frequency: " +  iterator.docsAndPositions( null, null ).freq()
                );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
