from ..service.tile import TileService
from ..application.provider import Singleton
from ..dataModel.tile import TileBase as TileDataModel

class Tile:

    def __new__(cls, scene_id: str, tile_id: str):
        """创建 Tile 实例，若 tile_id 不存在，则返回 None"""
        tile_service: TileService = Singleton.get_instance(id="tile_service")
        data: TileDataModel = tile_service.get_tile_by_id(scene_id, tile_id)
        if data is None:
            return None 
        print('Tile new !!!!!')
        instance = super().__new__(cls) 
        instance._data = data
        instance._scene_id = scene_id
        return instance
    
    def __init__(self, scene_id: str, tile_id: str):
        self._tile_service : TileService = Singleton.get_instance(id="tile_service")
        # self._data : TileDataModel = self._tile_service.get_tile_by_id(scene_id, tile_id)

    @classmethod
    def from_data_model(cls, data: TileDataModel, scene_id: str):
        instance = super().__new__(cls) 
        instance._data = data
        instance._scene_id = scene_id
        return instance

    # 基础映射属性
    @property
    def tile_id(self) -> str:
        return self._data.tile_id
    
    @property
    def image_id(self) -> str:
        return self._data.image_id
    
    @property
    def tile_level(self) -> int:
        return self._data.tile_level
    
    @property
    def column_id(self) -> int:
        return self._data.column_id
    
    @property
    def row_id(self) -> int:
        return self._data.row_id
    
    @property
    def path(self) -> str:
        return self._data.path
    
    @property
    def bucket(self) -> str:
        return self._data.bucket
    
    @property
    def cloud(self) -> float:
        return self._data.cloud
    
    def __repr__(self):
        return f"Tile(tile_id={self.tile_id}, image_id={self.image_id}, tile_level={self.tile_level}, column_id={self.column_id}, row_id={self.row_id}, path={self.path}, bucket={self.bucket}, cloud={self.cloud})"

    # 类的个性化方法
    def pull(self, output_path: str):
        return self._tile_service.pull_tile_by_id(self.image_id, self.tile_id, output_path)


