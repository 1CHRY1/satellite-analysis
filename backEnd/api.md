### 1、卫星数据接口调用

#### 1.1 传感器数据

##### （1）获取所有传感器信息

##### GET

```
/api/v1/data/sensor
```

##### Response

```
[
	{
		"sensorId":"",
		"sensorName":"",
		"platformName":""
	},
	...
]
```

##### (2) 获取传感器详情

##### GET

```
/api/v1/data/sensor/description/sensorId/{sensorId}
```

##### Response

```
{
	"description",
	...
}
```

#### 1.2 传感器产品数据

##### （1）根据传感器id获取产品信息

##### GET

```
/api/v1/data/product/sensorId/{sensorId}
```

##### Response

```
[
	{
		"productId":"",
		"productName":""
	},
	...
]
```

##### (2) 获取传感器产品详情

##### GET

```
/api/v1/data/product/description/productId/{productId}
```

##### Response

```
[
	{
		"description":"",
		"resolution":"",
		"period":""
	},
	...
}
```

#### 