from ..service.image import ImageService 
from ..application.provider import Singleton
from ..dataModel.image import Image as ImageDataModel
    
class Image:
    
    @staticmethod
    def query(scene_id: str = None, band: str = None, cloud_range: tuple[float, float] = None):
        image_service: ImageService = Singleton.get_instance(id="image_service")
        return [Image(image.image_id) for image in image_service.get_images(scene_id=scene_id, band=band, cloud_range=cloud_range)]
    
    def __new__(cls, image_id: str):
        """创建 Image 实例，若 image_id 不存在，则返回 None"""
        image_service: ImageService = Singleton.get_instance(id="image_service")
        data: ImageDataModel = image_service.get_image(image_id)
        
        if data is None:
            return None
        
        instance = super().__new__(cls) 
        instance._data = data
        return instance

    def __init__(self, image_id: str):
        """初始化 Image 对象"""
        self._image_service : ImageService = Singleton.get_instance(id="image_service") 

    # 基础映射属性
    @property
    def image_id(self) -> str:
        return self._data.image_id

    @property
    def scene_id(self) -> str:
        return self._data.scene_id

    @property
    def tif_path(self) -> str:
        return self._data.tif_path

    @property
    def band(self) -> str:
        return self._data.band

    @property
    def bucket(self) -> str:
        return self._data.bucket

    @property
    def cloud(self) -> float:
        return self._data.cloud
    
    @property
    def url(self) -> str:
        return f"/{self.bucket}/{self.tif_path}"
    
    def __repr__(self):
        return f"Image(image_id={self.image_id}, scene_id={self.scene_id}, tif_path={self.tif_path}, band={self.band}, bucket={self.bucket}, cloud={self.cloud})"


    # 类的个性化方法
    def pull(self, output_path: str):
        self._image_service.pull_image(self._data, output_path)