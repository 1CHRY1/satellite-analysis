## 1、整体内容

国土卫星资源分析中心项目部署分为多个模块：

**前端nginx服务**、**后端Java服务**、

**mongodb模型数据服务**、**pg空间数据服务**、**mysql卫星元数据服务**、**minio对象存储数据服务**、**elasticsearch地名数据服务**、**redis模型运行数据服务**、

**python模型分析服务**、**瓦片可视化服务（老版，新版）**、**瓦片可视化负载均衡服务**、**在线编程容器服务**

下面将分容器介绍部署方式。

---

###### 数据库模块：

## 2、mysql数据库部署操作步骤

#### 容器介绍

该容器为卫星元数据服务，用于存储所有传感器、产品、卫星影像的元数据，也用于存储在线编程项目以及分析数据的元数据。

#### 导入容器

```
docker load < mysql.tar
```

#### 启动容器

这里需要将 */mnt/volumes/mysql/data* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name mysql -p 3306:3306 -v /mnt/volumes/mysql/data:/var/lib/mysql --restart=always --privileged=true -e MYSQL_ROOT_PASSWORD=ogms250410 mysql:8.4-arm
```

```
 login: mysql -u root -p
 create: create database satellite // create database tile
 use: use satellite // use tile
 import: SOURCE /var/lib/mysql/satellite.sql // SOURCE /var/lib/mysql/tile.sql
```



## 3、mongo数据库部署操作步骤

#### 容器介绍

该容器为模型数据服务，用于存储所有组内模型元数据。

#### 导入容器

```
docker load < mongo.tar
```

#### 启动容器

这里需要将 */mnt/volumes/elasticsearch/data* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name mongo -p 27017:27017 -v /mnt/volumes/mongodb/data:/data/db --restart=always -e MONGO_INITDB_ROOT_USERNAME=OGMS  -e MONGO_INITDB_ROOT_PASSWORD=ogms250410 --privileged=true mongo:6-arm
```



## 4、minio数据库部署操作步骤

#### 容器介绍

该容器为卫星数据对象存储服务，用于存储各类卫星影像、模型计算结果的原数据。

#### 导入容器

```
docker load < minio.tar
```

#### 启动容器

这里需要将 */mnt_ard/volumes/minio/data* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name minio -p 9000:9000 -p 9001:9001 -v /mnt_ard/volumes/minio/data:/data --restart=always --privileged=true -e MINIO_ROOT_USER=OGMS -e MINIO_ROOT_PASSWORD=ogms250410 minio:arm server /data --console-address ":9001"
```

---



## 5、redis数据库部署操作步骤

#### 容器介绍

该容器为模型计算状态数据服务，结合后端的模型计算调度框架，存储后端与模型服务交互的模型计算状态与结果信息。

#### 导入容器

```
docker load < redis.tar
```

#### 启动容器

这里需要将 */mnt/volumes/redis/data* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name redis -p 6379:6379 -v /mnt/volumes/redis/data:/data --restart=always --privileged=true redis:arm
```

---



## 6、elasticsearch数据库部署操作步骤

#### 容器介绍

该容器为地名检索数据库服务，用于存储国土卫星系统中全国地名数据。

#### 导入容器

```
docker load < elasticsearch.tar
```

#### 启动容器

这里需要将 */mnt/volumes/elasticsearch/data* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name elasticsearch -e "discovery.type=single-node" -e "xpack.security.enabled=false" -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" -p 9200:9200 -p 9300:9300 -v /mnt/volumes/elasticsearch/data:/usr/share/elasticsearch/data --restart=always --privileged=true elasticsearch:8.18.1
```

---



## 7、pg数据库部署操作步骤

#### 容器介绍

该容器为矢量空间数据库服务，用于存储国土卫星系统中各类空间数据。

#### 导入容器

```
docker load < postgresql.tar
```

#### 启动容器

这里需要将 */mnt/volumes/postgresql/data* 替换为本机所挂载数据卷路径。

```cmd
amd版
docker run -d --name pg --restart=always -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=ogms250410 -e POSTGRES_DB=gis_db -p 5432:5432 -v /mnt/volumes/postgresql/data:/var/lib/postgresql/data --restart=always --privileged=true postgresql:postgis
```

```cmd
arm版 (问就是dmk选的非官方镜像)
docker run -d --name pg --restart=always -e POSTGRES_USER=postgres -e POSTGRES_PASS=ogms250410 -e POSTGRES_DBNAME=gis_db -p 5432:5432 -v /mnt/volumes/postgresql/data:/var/lib/postgresql/data --restart=always --privileged=true postgresql:postgis
```

---



###### 后台服务模块：

## 8、Java后端服务部署操作步骤

#### 容器介绍

该容器系统整体后端服务（另有微服务版本）。

#### 导入容器

```
docker load < java.tar
```

#### 启动容器

yaml配置需要注意mysql的密码认证方式
在url后加入：  **?useSSL=false&allowPublicKeyRetrieval=true**

这里需要将 */mnt/volumes/back* 替换为本机所挂载数据卷路径。其中8999为后台服务端口，9888为websocket连接端口。

```cmd
docker run -d --name back -p 8999:8999  -p 9888:9888 -v /mnt/volumes/back:/usr/resource --restart=always --privileged=true --network=host java:17-arm java -Dfile.encoding=UTF-8 -jar /usr/resource/satellite.jar
```

---



## 9、Python模型服务部署操作步骤

#### 容器介绍

该容器系统整体后端服务（另有微服务版本）。

#### 导入镜像

```
docker load -i < model-server.tar
```

#### 常见操作

```
docker ps -a
docker ps
docker rm name/id
net stop winnat
net start winnat
```

#### 启动容器

这里需要将 */mnt/volumes/modelServer* 替换为本机所挂载数据卷路径。

普通版本

```cmd
# version 1
docker run -d --name model-server -p 5000:5000 -v /mnt/volumes/modelServer:/usr/resource -w /usr/resource --restart=always --privileged=true model-server:0514 python -m dataProcessing.server
```

分布式计算版本（注意这里要基于宿主机设置cpu核数和内存占用数）

```cmd
# 北京创建ray-net错误，暂用host网络
docker network create ray-net
# version 2 distribution
# 启动 Head 节点容器
docker run -d --name model-server -p 5000:5000 -p 6379:6379 -p 8265:8265 -v /mnt/volumes/modelServer:/usr/resource --network=host -w /usr/resource --restart=always --privileged=true --shm-size=10gb model-server:0624 bash -c "ray start --include-dashboard=True --head --node-ip-address=0.0.0.0 --port=6379 --dashboard-host=0.0.0.0 --num-cpus=36 && python -m dataProcessing.server"

--memory=36000000000 --object-store-memory=12500000000  # 单位 byte，十进制

docker run -d --name ray-worker --shm-size=5.09gb --network=host -v /mnt/volumes/modelServer:/usr/resource model-server:0624 bash -c "ray start --address='172.31.13.23:6379' --block"
```



---



## 10、tiler瓦片生成器（老版）部署操作步骤

#### 容器介绍

该容器系统卫星数据瓦片化展示模块，为较老版本，主要作用与在线编程模块的数据可视化。

#### 导入容器

```
docker load < tiler.tar
```

#### 启动容器

这里需要将 */mnt/volumes/tiler* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name tiler -p 8079:8079 -v /mnt/volumes/tiler:/usr/resource --restart=always --privileged=true tiler:arm /usr/resource/start-tiler.sh
```

shell脚本内容为：

```cmd
python ./mintiler.py --minio_endpoint localhost:9000 --minio_access_key jTbgNHEqQafOpUxVg7Ol --minio_secret_key 7UxtrqhSOyN1KUeumbqTRMv1zeluLO69OwJnCC0M --port 8079
```

---



## 11、titiler瓦片生成器（新版）部署操作步骤

#### 容器介绍

该容器系统卫星数据瓦片化展示模块，为新版本，承载了系统中所有卫星数据的动态瓦片化展示。

注意！需要修改mosaic.py中的MinIO配置信息

#### 导入容器

```cmd
docker load < titiler.tar
```

#### 启动容器

这里需要将 */mnt/volumes/titiler* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name titiler -p 5050:8000 -v /mnt/volumes/titiler:/usr/resource --restart=always --privileged=true -w /usr/resource/version2 tiler:0514 uvicorn main:app --host 0.0.0.0
```

---



## 12、titiler负载均衡器部署操作步骤（nginx）



#### 容器介绍

由于10中的titiler作为系统服务模块起着至关重要的作用，且在使用中瓦片加载需要占用大量的服务带宽。而它具有着明显的无状态性，因此我们可以使用多个titiler服务作为系统的可视化支撑。titiler负载均衡器能够将大量请求分发到多个titiler，使其能够丝滑实时展示数据。这个过程在k8s中可以通过pod的弹性伸缩完成，而容器化部署中则更多需要使用nginx实现手动负载均衡。

#### 导入容器

```cmd
docker load < nginx.tar
```

#### 启动容器

这里需要将 */mnt/volumes/titiler_nginx/conf/nginx.conf* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name titiler_nginx -p 5050:8000 -v /mnt/volumes/titiler_nginx/conf/nginx.conf:/etc/nginx/nginx.conf --restart=always --privileged=true nginx:alpine-arm
```

---



## 13、地图服务部署操作步骤（nginx）

#### 容器介绍

由于系统部署在北京卫星遥感应用中心，内网环境下无法使用在线的地图服务，因此我们将一整套地图瓦片放在内网，并使用地图服务支撑其应用展示。

#### 导入容器

```cmd
docker load < base-map-service.tar
```

#### 启动容器

这里需要将 */mnt/volumes/base-map-service* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name basemap-vector -p 5002:5002 -v /mnt/volumes/base-map-service:/usr/resource --restart=always --privileged=true base-map-service:arm python /usr/resource/server_vector.py
```

---



## 14、在线编程docker服务部署操作步骤

该服务为docker服务器，可以接收外部指令使用指定镜像，启动服务器中的容器并运行计算代码，具体操作网上搜索即可。



###### 前端展示模块：

## 15、系统前端部署操作步骤（nginx）

#### 容器介绍

该服务为系统前端应用，将vue项目打包放在html文件夹中替换即可

#### 导入容器

```
docker load < nginx.tar
```

#### 启动容器

这里需要将 */usr/share/nginx* 替换为本机所挂载数据卷路径。

```cmd
docker run -d --name front -p 5173:5173 -v /mnt/volumes/front/html:/usr/share/nginx/html -v /mnt/volumes/front/conf/nginx.conf:/etc/nginx/nginx.conf --restart=always --privileged=true --network=host nginx:alpine-arm
```



