import requests
from scene import Scene

class SceneFetcher:
    def __init__(self, email, password):
        # 初始化需要的用户凭证
        self.email = email
        self.password = password
        self.token = None  # 用于存储 accessToken
        self.refresh_token = None  # 可选: 用于刷新 token
        self.user_id = None  # 用于存储用户 ID
        self.url_prefix = "http://172.31.13.21:8999/api/"

    def login(self):
        """进行登录请求并获取 token"""
        login_url = self.url_prefix + "v1/user/login"
        login_data = {
            "email": self.email,
            "password": self.password
        }
        
        # 发送登录请求
        response = requests.post(login_url, json=login_data)

        if response.status_code == 200:
            response_data = response.json()
            self.token = response_data.get("data", {}).get("accessToken")  # 获取 accessToken
            self.user_id = response_data.get("data", {}).get("userId")  # 获取 userId
            self.refresh_token = response_data.get("data", {}).get("refreshToken")  # 获取 refreshToken
            
            if self.token:
                pass
            else:
                print("Error: accessToken not found in response.", flush=True)
        else:
            print(f"Login failed with status code: {response.status_code}", flush=True)
            response.raise_for_status()

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
                    print(f"Grid {row_id}-{column_id} does not have valid coordinates.", flush=True)
            else:
                print(f"Unexpected data format: {grid}", flush=True)
        
        # 处理 geoJson 字段（如果需要的话）
        geo_json = grids_data.get('geoJson', {})
        
        # 返回所有格网的列表
        return grid_data

    def get_grids(self, region_id, resolution):
        """请求生成格网"""
        if not self.token:
            raise ValueError("Token is not available. Please login first.")

        grids_url = self.url_prefix + f"v1/data/grid/grids/region/{region_id}/resolution/{resolution}"
        headers = {
            "Authorization": f"Bearer {self.token}"
        }

        # 发起请求获取格网数据
        response = requests.get(grids_url, headers=headers)
        
        if response.status_code == 200:
            grid_data = response.json()
            grid_data = self.parse_grids(grid_data)
            return grid_data
        else:
            print(f"Failed to fetch grids with status code: {response.status_code}", flush=True)
            response.raise_for_status()

    def submit_query(self, start_time, end_time, region_id, resolution):
        """请求获取检索数据"""
        if not self.token:
            raise ValueError("Token is not available. Please login first.")

        data_url = self.url_prefix + "v3/data/scene/time/region"
        headers = {
            "Authorization": f"Bearer {self.token}"
        }
        data_payload = {
            "startTime": start_time,
            "endTime": end_time,
            "regionId": region_id,
            "resolution": resolution
        }
        
        # 发起请求获取数据
        response = requests.post(data_url, json=data_payload, headers=headers)
        
        if response.status_code == 200:
            # 从响应中获取 Cookie (encrypted_request_body)
            self.cookie = response.cookies.get("encrypted_request_body")
            print(f"Data retrieval successful. Cookie: {self.cookie}", flush=True)
            return response.json()
        else:
            print(f"Failed to retrieve data with status code: {response.status_code}", flush=True)
            response.raise_for_status()

    def get_scenes_for_grid(self, sensor_name, coords):
        """根据传感器名称和格网范围，获取影像路径"""
        if not self.token or not self.cookie:
            raise ValueError("Token and Cookie are required. Please complete previous steps.")

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

        scenes_url = self.url_prefix + "v3/modeling/example/scenes/visualization"
        headers = {
            "Authorization": f"Bearer {self.token}"
        }
        scenes_payload = {
            "sensorName": sensor_name,
            "points": [x_min, y_min, x_max, y_max]
        }
        
        # 携带 token 和 cookie 发起请求
        cookies = {
            "encrypted_request_body": self.cookie
        }

        # 发起请求获取影像路径
        response = requests.post(scenes_url, json=scenes_payload, headers=headers, cookies=cookies)
        
        if response.status_code == 200:
            # 解析数据并将每个场景数据转换为 Scene 对象
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
            print(f"Failed to retrieve scenes with status code: {response.status_code}", flush=True)
            response.raise_for_status()

# 示例使用：
if __name__ == "__main__":
    # 实例化 SceneFetcher 类
    fetcher = SceneFetcher(email="loop@ogms.com", password="ogms")
    
    # 登录并获取 token
    fetcher.login()
    
    # 获取格网数据
    grids_data = fetcher.get_grids(region_id="100000", resolution=10000)

    # 获取检索数据
    retrieved_data = fetcher.submit_query(
        start_time="2024-05-01", end_time="2025-07-30", region_id="100000", resolution=100
    )
    
    # 获取影像路径
    sensor_name = "Month_map"
    # points = grids_data[0]['coordinates'][0]

    scenes = fetcher.get_scenes_for_grid(sensor_name, grids_data[1]['coordinates'][0])
