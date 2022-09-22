package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;


public class HotelSearchTest {
    private RestHighLevelClient client;

    private void handleResponse(SearchResponse response) {
        //4. 解析结果
        SearchHits searchHits = response.getHits();
        //4,1 查询总条数
        long total = searchHits.getTotalHits().value;
        //4.2 查询的结果数组
        SearchHit[] hits = searchHits.getHits();
        System.out.println(total);
        for (SearchHit hit:hits){
            // 4.3 得到source
            String source = hit.getSourceAsString();

            //反序列化
            HotelDoc doc = JSON.parseObject(source, HotelDoc.class);
            // 获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(!CollectionUtils.isEmpty(highlightFields)) {
                // 根据字段名获取高亮结果
                HighlightField highlightField = highlightFields.get("name");
                //获取高亮值
                if (highlightField!=null) {
                    String name = highlightField.getFragments()[0].string();
                    // 覆盖高亮结果
                    doc.setName(name);
                }

            }
            // 4.4 打印结果
            System.out.println("hotelDoc="+doc);
        }
    }


    @Test
    void testMatchAll() throws IOException {
        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析响应
        handleResponse(response);

    }



    @Test
    void testMatch() throws IOException {
        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        request.source().query(QueryBuilders.matchQuery("name","如家"));
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);

    }

    @Test
    void testTerm() throws IOException {
        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        request.source().query(QueryBuilders.termQuery("city","上海"));
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);

    }

    @Test
    void testRange() throws IOException {
        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        request.source().query(QueryBuilders.rangeQuery("price").gte(100).lte(300));
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);

    }

    /*复合查询*/
    @Test
    void testBool() throws IOException {
        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        //2.1 准备BoolQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //2.2 添加term
        boolQuery.must(QueryBuilders.termQuery("city","北京"));
        //2.3 添加range
        boolQuery.filter(QueryBuilders.rangeQuery("price").gte(200).lte(1000));
        request.source().query(boolQuery);
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);

    }


    /*排序和分页*/
    @Test
    void testPageAndSort() throws IOException {
        // 页码，每页大小
        int page=2,size=5;
        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        //2.1 准备query
        request.source().query(QueryBuilders.matchAllQuery());
        //2.2 排序sort
        request.source().sort("price", SortOrder.DESC);
        //2.3 分页
        request.source().from((page-1)*size).size(size);
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);
    }
    @Test
    void testHighLight() throws IOException {

        //1. 准备request
        SearchRequest request = new SearchRequest("hotel");
        //2. 准备DSL
        //2.1 准备query
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        //2.2 高亮
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
        //3. 发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4. 解析
        handleResponse(response);
    }

    @BeforeEach
    void setUp(){
        this.client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.229.101:9200")
        ));
    }
        @AfterEach
        void tearDown() throws IOException {
            this.client.close();

    }
}
