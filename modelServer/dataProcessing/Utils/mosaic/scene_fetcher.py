import requests
from .scene import Scene
from dataProcessing.config import current_config as CONFIG

class SceneFetcher:
    def __init__(self, headers, cookies):
        # 初始化需要的用户凭证
        self.headers = headers
        self.cookies = cookies
        # self.url_prefix = "http://223.2.34.8:31584/api/"
        self.url_prefix = CONFIG.BACK_URL_PREFIX

    def parse_grids(self, grids_data):
        """解析返回的格网数据并返回格式化的结果"""
        grid_data = []

        # 处理 grids 字段
        grids = grids_data.get('grids', [])
        
        for grid in grids:
            if isinstance(grid, dict):  # 确保 grid 是字典对象
                row_id = grid.get('rowId')
                column_id = grid.get('columnId')
                resolution = grid.get('resolution')
                boundary = grid.get('boundary', {})
                geometry = boundary.get('geometry', {})
                coordinates = geometry.get('coordinates', [])

                # 如果有有效的坐标数据
                if coordinates:
                    # 格式化每个格网的信息
                    grid_dict = {
                        'rowId': row_id,
                        'columnId': column_id,
                        'coordinates': coordinates
                    }

                    # 将格式化后的格网信息添加到列表中
                    grid_data.append(grid_dict)
                else:
                    print(f"Grid {row_id}-{column_id} does not have valid coordinates.")
            else:
                print(f"Unexpected data format: {grid}")
        
        # 处理 geoJson 字段（如果需要的话）
        geo_json = grids_data.get('geoJson', {})
        
        # 返回所有格网的列表
        return grid_data

    def get_scenes_for_grid(self, sensor_name, coords):
        """根据传感器名称和格网范围，获取影像路径"""

        # 初始化最小最大值
        x_min = y_min = float('inf')
        x_max = y_max = float('-inf')

        # 遍历 coordinates 获取最小最大值
        for point in coords:
            x, y = point
            if x < x_min:
                x_min = x
            if x > x_max:
                x_max = x
            if y < y_min:
                y_min = y
            if y > y_max:
                y_max = y

        scenes_url = self.url_prefix + CONFIG.
        
        headers = self.headers
        cookies = self.cookies
        scenes_payload = {
            "sensorName": sensor_name,
            "points": [x_min, y_min, x_max, y_max]
        }
        # 发起请求获取影像路径
        response = requests.post(scenes_url, json=scenes_payload, headers=headers, cookies=cookies)
        
        if response.status_code == 200:
            # 解析数据并将每个场景数据转换为 Scene 对象
            print(response.json()['data'], flush = True)
            scenes_data = response.json()['data']['scenesConfig']
            band_mapper = response.json()['data']['bandMapper']
            scene_objects = []
            for scene in scenes_data:
                scene_obj = Scene(
                    scene_id=scene['sceneId'],
                    sensor_name=scene['sensorName'],
                    resolution=scene['resolution'],
                    cloud_mask_path=scene.get('cloudPath', None),
                    bucket=scene['bucket'],
                    no_data=scene['noData'],
                    bbox=scene['bbox']['geometry']['coordinates'][0],
                    path=scene['path'],
                    cloud=scene['cloud'],
                    coverage=scene['coverage'],
                    band_mapper=band_mapper
                )
                scene_objects.append(scene_obj)
            
            return scene_objects
        else:
            print(f"Failed to retrieve scenes with status code: {response.status_code}")
            response.raise_for_status()
            return None