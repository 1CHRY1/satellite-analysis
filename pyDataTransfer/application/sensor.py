from service.sensor import SensorService 
from service.product import ProductService
from application.provider import Singleton
from application.product import Product
from dataModel.sensor import Sensor as SensorDataModel

class Sensor:
    
    def __new__(cls, sensor_id: str):
        """创建 Sensor 实例，若 sensor_id 不存在，则返回 None"""
        sensor_service: SensorService = Singleton.get_instance(id="sensor_service")
        data: SensorDataModel = sensor_service.get_by_id(sensor_id)
        
        if data is None:
            return None 
        
        instance = super().__new__(cls) 
        instance._data = data
        return instance

    def __init__(self, sensor_id: str):
        """初始化 Sensor 对象"""
        self._sensor_service : SensorService = Singleton.get_instance(id="sensor_service") 
        self._product_service : ProductService = Singleton.get_instance(id="product_service")
        # self._data : SensorDataModel = self._sensor_service.get_by_id(sensor_id)

    # 基础映射属性
    @property
    def sensor_id(self) -> str:
        return self._data.sensor_id

    @property
    def sensor_name(self) -> str:
        return self._data.sensor_name

    @property
    def platform_name(self) -> str:
        return self._data.platform_name

    @property
    def description(self) -> str:
        return self._data.description
    
    def __repr__(self):
        return f"Sensor(sensor_id={self.sensor_id}, sensor_name={self.sensor_name}, platform_name={self.platform_name}, description={self.description})"    


    # 类的个性化方法
    def get_all_products(self):
        products = self._product_service.filter_by_sensor_id(self.sensor_id)
        return [Product(product.product_id) for product in products]




