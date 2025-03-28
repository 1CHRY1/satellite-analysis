from ..service.product import ProductService 
from ..service.scene import SceneService
from ..application.provider import Singleton
from ..application.scene import Scene
from ..dataModel.product import Product as ProductDataModel

class Product:

    def __new__(cls, product_id: str):
        """创建 Product 实例，若 product_id 不存在，则返回 None"""
        product_service: ProductService = Singleton.get_instance(id="product_service")
        data: ProductDataModel = product_service.get_by_id(product_id)
        
        if data is None:
            return None
        
        instance = super().__new__(cls) 
        instance._data = data
        return instance


    def __init__(self, product_id: str):
        """初始化 Product 对象"""
        self._product_service : ProductService = Singleton.get_instance(id="product_service") 
        self._scene_service : SceneService = Singleton.get_instance(id="scene_service")
        # self._data : ProductDataModel = self._product_service.get_by_id(product_id)

    # 基础映射属性
    @property
    def product_id(self) -> str:
        return self._data.product_id

    @property
    def sensor_id(self) -> str:
        return self._data.sensor_id

    @property
    def product_name(self) -> str:
        return self._data.product_name

    @property
    def description(self) -> str:
        return self._data.description

    @property
    def resolution(self) -> str:
        return self._data.resolution

    @property
    def period(self) -> int:
        return self._data.period
    
    def __repr__(self):
        return f"Product(product_id={self.product_id}, sensor_id={self.sensor_id}, product_name={self.product_name}, description={self.description}, resolution={self.resolution}, period={self.period})"    


    # 类的个性化方法
    def get_all_scenes(self):
        scenes = self._scene_service.filter_by_product_id(self.product_id)
        return [Scene(scene.scene_id) for scene in scenes]