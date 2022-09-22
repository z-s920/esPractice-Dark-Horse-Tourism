package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        try {
            //1. 准备request
            SearchRequest request = new SearchRequest("hotel");
            //2. 准备DSL
            //2.1 query
            //构建一个boolean query
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            // 全文检索 must部分  关键字搜索
            String key = params.getKey();//前端获取的条件字段内容
            if (key == null || "".equals(key)) {
                boolQuery.must(QueryBuilders.matchAllQuery());//即没有条件  条件内容为空时 就查询所有内容。
            } else {
                boolQuery.must(QueryBuilders.matchQuery("all", key));
            }
            //条件过滤——城市
            if (params.getCity()!=null&&!params.getCity().equals("")){
                boolQuery.filter(QueryBuilders.termQuery("city",params.getCity()));
            }
            //条件过滤——品牌
            if (params.getBrand()!=null&&!params.getBrand().equals("")){
                boolQuery.filter(QueryBuilders.termQuery("brand",params.getBrand()));
            }

            //条件过滤——星级
            if (params.getStarName()!=null&&!params.getStarName().equals("")){
                boolQuery.filter(QueryBuilders.termQuery("StarName",params.getStarName()));
            }

            //条件过滤——价格
            if (params.getMinPrice()!= null && params.getMaxPrice()!=null){
                boolQuery.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
            }

            // 算分控制
            FunctionScoreQueryBuilder functionScoreQuery =
                    QueryBuilders.functionScoreQuery(
                            //原始查询，相关性算分的查询
                            boolQuery,
                            //function socre的数组
                            new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                    //其中一个function score 元素
                                 new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                         // 过滤条件
                                            QueryBuilders.termQuery("isAD","true"),
                                    //算分函数
                                    ScoreFunctionBuilders.weightFactorFunction(10)


                            )
                    });
            request.source().query(functionScoreQuery);
            //2.2 分页

            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);

            //2.3 排序
            String location = params.getLocation();
            if (location!=null&& !location.equals("")){
                request.source().sort(SortBuilders.geoDistanceSort("location",new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)

                );
            }
            //3. 发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            //4. 解析
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private PageResult handleResponse(SearchResponse response) {
        //4. 解析结果
        SearchHits searchHits = response.getHits();
        //4,1 查询总条数
        long total = searchHits.getTotalHits().value;
        //4.2 查询的结果数组
        SearchHit[] hits = searchHits.getHits();
       //4.3 遍历
        List<HotelDoc> hotels=new ArrayList<>();

        for (SearchHit hit : hits) {
            // 得到source
            String json = hit.getSourceAsString();

            //反序列化
            HotelDoc hoteldoc = JSON.parseObject(json, HotelDoc.class);

            //获得排序值
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length>0){
                Object sortValue = sortValues[0];
                hoteldoc.setDistance(sortValue);
            }
            hotels.add(hoteldoc);

        }

        //4.4 封装返回

        return new PageResult(total,hotels);
    }
}

