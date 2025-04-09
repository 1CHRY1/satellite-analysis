from ..service.scene import SceneService 
from ..service.image import ImageService
from ..service.tile import TileService
from ..application.provider import Singleton
from ..application.image import Image
from ..application.tile import Tile
from ..dataModel.scene import Scene as SceneDataModel
from datetime import datetime
from dataclasses import dataclass
@dataclass
class GridCell:
    columnId: int
    rowId: int

class Scene:
    
    @staticmethod
    def query(scene_name: str = None, product_id: str = None, polygon: object = None, time_range: tuple[datetime, datetime] = None, cloud_range: tuple[float, float] = None):
        scene_service: SceneService = Singleton.get_instance(id="scene_service")
        return [Scene(scene.scene_id) for scene in scene_service.get_scenes(scene_name, product_id, polygon, time_range, cloud_range)]

    def __new__(cls, scene_id: str):
        """创建 Scene 实例，若 scene_id 不存在，则返回 None"""
        scene_service: SceneService = Singleton.get_instance(id="scene_service")
        data: SceneDataModel = scene_service.get_scene(scene_id)
        
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
        images = self._image_service.get_images(scene_id=self.scene_id)
        return [Image(image.image_id) for image in images]
    
    def get_band_image(self, band: str):
        image = self._image_service.get_images(scene_id=self.scene_id, band=band)
        return Image(image.image_id)
    
    def get_tile(self, tile_id: str):
        tile = self._tile_service.get_tile(scene_id=self.scene_id, tile_id=tile_id)
        return Tile.from_data_model(tile, self.scene_id)
    
    def get_tiles(self, tile_ids: list[str] = None, cloud_range: tuple[float, float] = None, polygon: object = None, tile_level: str = None, band: int = None, grid_cells: list[GridCell] = None):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, tile_ids=tile_ids, cloud_range=cloud_range, polygon=polygon, tile_level=tile_level, band=band, grid_cells=grid_cells)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_all_tiles(self):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]

    def get_tiles_by_cloud(self, maxcloud: float):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, cloud_range=(0, maxcloud))
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def pull_tile(self, tile_id: str, output_path: str):
        self._tile_service.pull_tile(tile_id=tile_id, output_path=output_path)
    
    def pull_tiles(self, tile_ids: list[str], output_dir: str):
        self._tile_service.pull_tiles(tile_ids=tile_ids, output_dir=output_dir)