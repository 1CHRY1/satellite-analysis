class Scene:
    def __init__(self, scene_id, sensor_name, resolution, cloud_mask_path, bucket, no_data, bbox, path, cloud, coverage, band_mapper):
        self.scene_id = scene_id
        self.sensor_name = sensor_name
        self.resolution = resolution
        self.cloud_mask_path = cloud_mask_path
        self.bucket = bucket
        self.no_data = no_data
        self.bbox = bbox
        self.cloud = cloud
        self.coverage = coverage
        self.band_mapper = band_mapper
        self.path = self.reorder_path(path)

    def reorder_path(self, path):
        """根据 bandMapper 中的顺序，重新排列路径"""
        try:
            # 按照 bandMapper 顺序排列 Red, Green, Blue
            sorted_path = []
            target_bands = ['Red', 'Green', 'Blue']
            
            for band in target_bands:
                if band in self.band_mapper:
                    band_key = f"band_{self.band_mapper[band]}"
                    if band_key in path and path[band_key] is not None:
                        sorted_path.append(path[band_key])
                    else:
                        print(f"Warning: {band_key} not found in path for scene {self.scene_id}")
                else:
                    print(f"Warning: {band} not found in band_mapper for scene {self.scene_id}")
            
            # 如果没有找到任何有效路径，返回原始路径的值列表
            if not sorted_path:
                print(f"Warning: No valid bands found, using original path values for scene {self.scene_id}")
                sorted_path = [v for v in path.values() if v is not None]
            
            return sorted_path
            
        except Exception as e:
            print(f"Error in reorder_path for scene {self.scene_id}: {e}")
            # 返回原始路径的值列表作为备选
            return [v for v in path.values() if v is not None]

    def __repr__(self):
        return f"Scene(scene_id={self.scene_id}, sensor_name={self.sensor_name}, " \
               f"resolution={self.resolution}, cloud={self.cloud}, coverage={self.coverage})"