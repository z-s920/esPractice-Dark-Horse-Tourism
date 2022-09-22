# esPractice-Dark-Horse-Tourism
Use the front and back end calls of a travel page to implement some query operations of es, such as accurate query, compliance query, full-text search, geographic query, etc
## Run the project
### 1. application.yaml：
  1.1 将数据库的URl中的数据库名改成本地数据库名字   
  1.2 将用户名密码修改成自己的
### 2. 虚拟机：
  2.1 运行本地虚拟机，部署单点es
  ```
  systemctl start docker //打开docker
  docker network create es-net //创建一个名为es-net的网络
  
//对上传的压缩包进行加载
docker load -i es.tar 
docker load -i kibana.tar

  docker run -d \   //-d：后台运行
	--name es \   // 顾名思义  起个名字给容器
    -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \   // -e 环境变量的配置
    -e "discovery.type=single-node" \       // 运行模式为单点运行，如果集群运行的话 这么需要修改
    -v es-data:/usr/share/elasticsearch/data \   // -v ：数据卷挂载
    -v es-plugins:/usr/share/elasticsearch/plugins \
    --privileged \
    --network es-net \  //将es容器加到es-net网络中
    -p 9200:9200 \   //-p暴露端口，9200暴露的http端口供用户使用，9300 各个节点互联的端口
    -p 9300:9300 \
elasticsearch:7.12.1   //镜像名称
```
  2.2 部署kibana： 可以给我们提供一个elasticsearch的可视化界面，便于我们学习，可以很好的编写DSL语句。
  ```
  docker run -d \
--name kibana \
-e ELASTICSEARCH_HOSTS=http://es:9200 \ //设置elasticsearch的地址，因为kibana已经与
//elasticsearch在一个网络，因此可以用容器名直接访问elasticsearch
--network=es-net \  //加入一个名为es-net的网络中，与elasticsearch在同一个网络中
-p 5601:5601  \ //端口映射配置
kibana:7.12.1 //一定要与es的版本保持一致
```
2.3 安装IK分词器:es在创建倒排索引时需要对文档分词；在搜索时，需要对用户输入内容分词。但默认的分词规则对中文处理并不友好。我们在kibana的DevTools中测试：
```

POST /_analyze  //请求方式+请求路径,这里省略了http://本机ip:9200，有kibana帮我们补充
 
{
  "analyzer": "standard",  //standard为默认分词器
  "text": "黑马程序员学习java太棒了！"
```
![image](https://user-images.githubusercontent.com/75167800/191643519-8691a6c2-7910-4fd1-9d1a-09a5a1fef988.png)

  
**会发现得到的结果它在分词时，英文分的还可以，中文的话是逐一分词，对我们查找文档信息不是很友好，这样的话比如说搜索“手机”关键词时，它将包含手或者机的都搜索出来了，但是结果并不是我们想要的。。。**  
### 分词器
处理中文分词，一般会使用IK分词器。https://github.com/medcl/elasticsearch-analysis-ik

ik分词器包含两种模式：

* ik_smart：最少切分，粗粒度
* ik_max_word：最细切分，细粒度
#### 安装IK分词器——安装ik插件（在线较慢)
```# 进入容器内部
docker exec -it elasticsearch /bin/bash
 
#### 在线下载并安装
./bin/elasticsearch-plugin  install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.12.1/elasticsearch-analysis-ik-7.12.1.zip
 
#退出
exit
#重启容器
docker restart elasticsearch
```
#### 安装IK分词器——离线安装ik插件（推荐）
  1. 查看数据卷目录
  安装插件需要知道elasticsearch的plugins目录位置，而我们用了数据卷挂载，因此需要查看elasticsearch的数据卷目录，通过下面命令查看:
  ```
  docker volume inspect es-plugins //Mountpoint:后面跟的就是被挂载的目录
  ```
  2. 上传到es容器的插件数据卷中
  首先cd 到该目录下然后利用xftp7传输文件到该目录
  3. 重启容器
  ```
  docker restart es
 
#### 查看es日志
docker logs -f es
  ```
  
  
### 3. 将数据库数据导入索引库中（利用了JavaRestClient中的Bulk批处理） 也就是项目中测试类中的testBulkRequest 函数，运行一下即可
### 4. 启动主函数HotelDemoApplication即可，在浏览器输入 localhost:8089 即可看到旅游页面
### Details

 可通过以下作者写的博客详细了解基础知识。
 上述文件中的es.tar kibana.tar文件较大，上传不了，可以在第一个博客中置顶的下载链接中下载
1. https://blog.csdn.net/m0_62025000/article/details/126865310?spm=1001.2014.3001.5501
2. https://blog.csdn.net/m0_62025000/article/details/126850080?spm=1001.2014.3001.5501
3. https://blog.csdn.net/m0_62025000/article/details/126902977?spm=1001.2014.3001.5501
