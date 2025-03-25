from service.scene import SceneService 
from service.image import ImageService
from service.tile import TileService
from application.provider import Singleton
from application.image import Image
from application.tile import Tile
from dataModel.scene import Scene as SceneDataModel

class Scene:

    def __new__(cls, scene_id: str):
        """创建 Scene 实例，若 scene_id 不存在，则返回 None"""
        scene_service: SceneService = Singleton.get_instance(id="scene_service")
        data: SceneDataModel = scene_service.get_by_id(scene_id)
        
        if data is None:
            return None 
        
        instance = super().__new__(cls) 
        instance._data = data
        return instance


    def __init__(self, scene_id: str):
        """初始化 Scene 对象"""
        self._scene_service : SceneService = Singleton.get_instance(id="scene_service") 
        self._image_service : ImageService = Singleton.get_instance(id="image_service")
        self._tile_service : TileService = Singleton.get_instance(id="tile_service")
        # self._data : SceneDataModel = self._scene_service.get_by_id(scene_id)

    # 基础映射属性
    @property
    def scene_id(self) -> str:
        return self._data.scene_id

    @property
    def product_id(self) -> str:
        return self._data.product_id

    @property
    def scene_name(self) -> str:
        return self._data.scene_name

    @property
    def scene_time(self) -> str:
        return self._data.scene_time

    @property
    def sensor_id(self) -> str:
        return self._data.sensor_id
    
    @property
    def tile_level_num(self) -> int:
        return self._data.tile_level_num
    
    @property
    def tile_levels(self) -> str:
        return self._data.tile_levels
    
    @property
    def coordinate_system(self) -> str:
        return self._data.coordinate_system
    
    @property
    def bounding_box(self) -> str:
        return self._data.bounding_box
    
    @property
    def description(self) -> str:
        return self._data.description
    
    @property
    def png_path(self) -> str:
        return self._data.png_path
    
    @property
    def bands(self) -> str:
        return self._data.bands
    
    @property
    def band_num(self) -> int:
        return self._data.band_num
    
    @property
    def bucket(self) -> str:
        return self._data.bucket
    
    @property
    def cloud(self) -> float:
        return self._data.cloud

    def __repr__(self):
        return f"Scene(scene_id={self.scene_id}, product_id={self.product_id}, scene_name={self.scene_name}, scene_time={self.scene_time}, sensor_id={self.sensor_id}, tile_level_num={self.tile_level_num}, tile_levels={self.tile_levels}, coordinate_system={self.coordinate_system}, bounding_box={self.bounding_box}, description={self.description}, png_path={self.png_path}, bands={self.bands}, band_num={self.band_num}, bucket={self.bucket}, cloud={self.cloud})"    


    # 类的个性化方法
    def get_all_band_images(self):
        images = self._image_service.filter_by_scene_id(self.scene_id)
        return [Image(image.image_id) for image in images]
    
    def get_band_image(self, band: str):
        image = self._image_service.filter_by_scene_id_and_band(self.scene_id, band)
        return Image(image.image_id)
    
    def get_all_tiles(self):
        tiles = self._tile_service.get_tiles_by_scene(self.scene_id)
        return [Tile(self.scene_id, tile.tile_id) for tile in tiles]
    
    def get_tile(self, tile_id: str):
        tile = self._tile_service.get_tile_by_id(self.scene_id, tile_id)
        return Tile(self.scene_id, tile.tile_id)
    
    def pull_tile_by_id(self, tile_id: str, output_path: str):
        self._tile_service.pull_tile_by_id(self.scene_id, tile_id, output_path)
    
    def pull_tiles_by_ids(self, tile_ids: list[str], output_dir: str):
        self._tile_service.pull_tiles_by_ids(self.scene_id, tile_ids, output_dir)
    
    def get_tiles_by_cloud(self, mincloud: float):
        tiles = self._tile_service.get_tiles_by_scene_and_cloud(self.scene_id, mincloud)
        return [Tile(self.scene_id, tile.tile_id) for tile in tiles]
