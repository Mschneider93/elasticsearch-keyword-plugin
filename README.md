elasticsearch-keyword-plugin
============================

Plugin for elasticsearch in order to determine keywords of documents


Requirements:

elasticsearch version > 1 because we need the termvector api for this plugin.

#Making an index with termvector information
https://github.com/elasticsearch/elasticsearch/issues/3114

#Getting Termvector information
curl -XGET 'http://127.0.0.1:9200/index/type/id/_termvector?pretty=true' -d '{ "fields":["content"], "term_statistics":true, "field_statistics":true, "offsets":false, "payloads":false, "positions":false}' | more


#Getting Keywords, this one searches for the url and uses the coresponding id to retrieve the termvector-information. Based on that the keywords are calculated
curl -XGET 'http://127.0.0.1:9200/index/type/_keyword' -d '{ "url": "http://www.hello.de", "keywordCalculationType":"LogTFIDFRank" }'
