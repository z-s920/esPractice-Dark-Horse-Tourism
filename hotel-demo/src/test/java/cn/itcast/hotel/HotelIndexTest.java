package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static cn.itcast.hotel.constants.HotelConstants.HotelSource;


public class HotelIndexTest {
    private RestHighLevelClient client;

    @Test
    void testInit(){
        System.out.println(client);
    }

    @Test
    void createHotelIndex() throws IOException {
        //1. 创建Request 对象
        CreateIndexRequest request = new CreateIndexRequest("hotel");
        //2. 准备请求的参数:DSL语句
        request.source(HotelSource, XContentType.JSON);
        //3. 发送请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void deleteHotelIndex() throws IOException {
        //1. 创建Request 对象
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("hotel");
        //2. 发送请求
        client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
    }

    @Test
    void existHotelIndex() throws IOException {
        //1. 创建Request 对象
        GetIndexRequest getIndexRequest = new GetIndexRequest("hotel");
        //2. 发送请求
        boolean exists=client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        //3. 输出是否存在
        System.out.println(exists?"索引库已存在!":"索引库不存在");
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
