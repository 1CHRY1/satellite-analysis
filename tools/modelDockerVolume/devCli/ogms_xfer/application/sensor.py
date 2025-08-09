from ..service.sensor import SensorService 
from ..service.product import ProductService
from ..application.provider import Singleton
from ..application.product import Product
from ..dataModel.sensor import Sensor as SensorDataModel

class Sensor:
    # 静态方法
    @staticmethod
    def all():
        sensor_service: SensorService = Singleton.get_instance(id="sensor_service")
        return [Sensor(sensor.sensor_id) for sensor in sensor_service.get_all()]
    
    @staticmethod
    def query(sensor_name: str = None, platform_name: str = None):
        sensor_service: SensorService = Singleton.get_instance(id="sensor_service")
        return [Sensor(sensor.sensor_id) for sensor in sensor_service.get_sensors(sensor_name, platform_name)]
    
    # __new__ 是一个静态方法，用于创建实例， 它在 __init__ 之前被调用，并且返回一个新的实例对象。
    # __init__ 是一个实例方法，用于初始化实例， 它在 __new__ 创建实例之后调用，主要用来给实例属性赋值。
    def __new__(cls, sensor_id: str):
        """创建 Sensor 实例，若 sensor_id 不存在，则返回 None"""
        sensor_service: SensorService = Singleton.get_instance(id="sensor_service")
        data: SensorDataModel = sensor_service.get_sensor(sensor_id)
        
        if data is None:
            return None 
        
        instance = super().__new__(cls) 
        instance._data = data
        return instance

    def __init__(self, sensor_id: str):
        """初始化 Sensor 对象"""
        self._sensor_service : SensorService = Singleton.get_instance(id="sensor_service") 
        self._product_service : ProductService = Singleton.get_instance(id="product_service")

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
        products = self._product_service.get_products(sensor_id=self.sensor_id)
        return [Product(product.product_id) for product in products]