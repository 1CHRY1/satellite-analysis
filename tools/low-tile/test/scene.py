

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
        # 按照 bandMapper 顺序排列 band_1, band_2, band_3, band_4
        sorted_path = []
        for band in ['Red', 'Green', 'Blue']:
            band_key = f"band_{self.band_mapper[band]}"
            sorted_path.append(path.get(band_key))
        return sorted_path

    def __repr__(self):
        return f"Scene(scene_id={self.scene_id}, sensor_name={self.sensor_name}, \
         resolution={self.resolution}, cloud={self.cloud}, coverage={self.coverage})"
