from ..service.image import ImageService 
from ..service.tile import TileService
from ..application.provider import Singleton
from ..application.tile import Tile
from ..dataModel.image import Image as ImageDataModel

from dataclasses import dataclass
@dataclass
class GridCell:
    columnId: int
    rowId: int
    
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
        self._tile_service : TileService = Singleton.get_instance(id="tile_service")

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
    
    def get_tile(self, tile_id: str):
        tile = self._tile_service.get_tile(scene_id=self.scene_id, tile_id=tile_id)
        return Tile.from_data_model(tile, self.scene_id)

    def get_all_tiles(self):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_tiles(self, ids: list[str] = None, max_cloud: float = None, polygon: object= None, tile_level: str = None):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, tile_ids=ids, cloud_range=(0, max_cloud), polygon=polygon, tile_level=tile_level)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]

    def get_tiles_by_ids(self, tile_ids: list[str]):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, tile_ids=tile_ids)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_tiles_by_cloud_range(self, min_cloud: float, max_cloud: float):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, cloud_range=(min_cloud, max_cloud))
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_tiles_by_cloud(self, max_cloud: float):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, cloud_range=(0, max_cloud))
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_tiles_by_polygon(self, polygon: object):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, polygon=polygon)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_tiles_by_grid_cells(self, grid_cells: list[GridCell]):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, grid_cells=grid_cells)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def get_tiles_by_tile_level(self, tile_level: str):
        tiles = self._tile_service.get_tiles(scene_id=self.scene_id, image_id=self.image_id, tile_level=tile_level)
        return [Tile.from_data_model(tile, self.scene_id) for tile in tiles]
    
    def pull_tile(self, tile_id: str, output_path: str):
        self._tile_service.pull_tile(scene_id=self.scene_id, tile_id=tile_id, output_path=output_path)
       
    def pull_tiles(self, tile_ids: list[str], output_dir: str):
        self._tile_service.pull_tiles(scene_id=self.scene_id, tile_ids=tile_ids, output_dir=output_dir)