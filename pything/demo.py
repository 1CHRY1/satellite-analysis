import json, os, time
from shapely.geometry import shape
from shapely.ops import unary_union
import requests
import rasterio
from rasterio.mask import mask
from rasterio.warp import transform_bounds
import numpy as np
from ogms_xfer.application.gridUtil import GridHelper
from ogms_xfer import OGMS_Xfer as xfer

xfer.initialize(os.path.join(os.path.dirname(__file__), "xferConfig.json"))


#### Helper functions #######################################################################
def get_geojson_bbox(geojson):

    def extract_geometries(obj):
        geometries = []
        if obj["type"] == "FeatureCollection":
            for feature in obj["features"]:
                geometries.extend(extract_geometries(feature))
        elif obj["type"] == "Feature":
            geometries.append(shape(obj["geometry"]))
        elif obj["type"] in ("Polygon", "MultiPolygon", "Point", "MultiPoint", "LineString", "MultiLineString"):
            geometries.append(shape(obj))
        else:
            raise ValueError(f"Unsupported GeoJSON type: {obj['type']}")
        return geometries

    geometries = extract_geometries(geojson)
    combined = unary_union(geometries)
    minx, miny, maxx, maxy = combined.bounds
    return [minx, miny, maxx, maxy]

def bbox_to_geojson(bbox):
    minx, miny, maxx, maxy = bbox
    return {
        "type": "FeatureCollection",
        "features": [
        {
            "type": "Feature",
            "geometry": {
                "type": "Polygon",
                "coordinates": [
                    [
                        [minx, miny],
                        [maxx, miny],
                        [maxx, maxy],
                        [minx, maxy],
                        [minx, miny]
                    ]
                ]
            }
        }]
    }

def bbox_to_geojsonFeatureGeometry(bbox):
    minx, miny, maxx, maxy = bbox
    return {
                "type": "Polygon",
                "coordinates": [
                    [
                        [minx, miny],
                        [maxx, miny],
                        [maxx, maxy],
                        [minx, maxy],
                        [minx, miny]
                    ]
                ]
        }

def calculate_cloud_coverage(image_path, bbox):

    with rasterio.open(image_path) as src:
        bbox_proj = transform_bounds(
            'EPSG:4326',  # WGS84
            src.crs,      # 图像的投影
            *bbox   # 解包 bbox: minx, miny, maxx, maxy
        )
        out_image, out_transform = mask(src, [bbox_to_geojsonFeatureGeometry(bbox_proj)], crop=True)
        out_meta = src.meta.copy()
        
    # 更新元数据
    out_meta.update({
        "driver": "GTiff",
        "height": out_image.shape[1],
        "width": out_image.shape[2],
        "transform": out_transform
    })

    cloud_mask = (out_image[0] & (1 << 3)) > 0  # 提取第3位

    cloud_pixels = cloud_mask.sum()
    total_pixels = out_image[0].size
    cloud_coverage = cloud_pixels / total_pixels
    
    print(f"云量百分比：{cloud_coverage * 100}%")
    return cloud_coverage



#### Main ########################################################################

grid_resolution_in_kilometer = 25

## Step 1 : 行政区范围 ————> 覆盖格网 ————> 检索影像
geojsonPath = "D:\\edgedownload\\胶州市.json"
f = open(geojsonPath, 'r', encoding='utf-8')
geojson_input = json.load(f)
f.close()

bbox = get_geojson_bbox(geojson_input)
bbox_geojson = bbox_to_geojson(bbox)
gridHelper = GridHelper(grid_resolution_in_kilometer)
grid_cells = gridHelper.get_grid_cells_by_bbox(bbox)
print("共",len(grid_cells),"个格网")

# 影像检索, 数据问题，现在先写死上传的山东landsat三景
scenes = [
    {
        "name":     'LC08_L2SP_120035_20250116_20250127_02_T1',
        "imageUrl": 'http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250116_20250127_02_T1_SR_B1.TIF',
        "qaUrl":    'http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250116_20250127_02_T1_QA_PIXEL.TIF',
    },{
        "name":     'LC08_L2SP_120035_20250217_20250226_02_T1',
        "imageUrl": 'http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250217_20250226_02_T1_SR_B1.TIF',
        "qaUrl":    'http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250217_20250226_02_T1_QA_PIXEL.TIF',
    },{
        "name":     'LC08_L2SP_120035_20250321_20250327_02_T1',
        "imageUrl": 'http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250321_20250327_02_T1_SR_B1.TIF',
        "qaUrl":    'http://223.2.43.228:30900/test-images/qa/LC08_L2SP_120035_20250321_20250327_02_T1_QA_PIXEL.TIF',
    }
]


# Step 2 : 遍历格网 & 遍历影像 ————> 实时切片并下载影像瓦片 ————> 采样QA计算云量 ————> 筛选出覆盖目标区域的云量最小的影像瓦片
target_tiles_map = {}
baseParams = {
    'bidx': '1',
    'unscale': 'false',
    'resampling': 'nearest',
    'reproject': 'nearest',
    'return_mask': 'false'
}
if True:
    start_time = time.time()
    for (idx, gcell) in enumerate(grid_cells):

        bbox = gridHelper.get_grid_bbox(gcell)

        for scene in scenes:
            params = baseParams.copy()
            params['url'] = scene['imageUrl']
            QAImagePath = scene['qaUrl']
            response = requests.get(f"http://127.0.0.1:8000/bbox/{bbox[0]},{bbox[1]},{bbox[2]},{bbox[3]}.tif", params=params)
            
            if response.status_code != 200:
                print(f'下载失败，状态码：{response.status_code}')
                continue
            grid_id = f"{grid_resolution_in_kilometer}_{gcell.columnId}_{gcell.rowId}"
            fname = './temp/' + f'{scene["name"]}_{grid_id}.tif'
            with open(fname, 'wb') as file:
                for chunk in response.iter_content(chunk_size=1024):
                    if chunk:
                        file.write(chunk)
            cloud = calculate_cloud_coverage(QAImagePath, bbox)
            
            if grid_id not in target_tiles_map:
                target_tiles_map[grid_id] = {
                    "grid_id": grid_id,
                    "cloud": cloud,
                    "file_path": fname,
                }

            elif target_tiles_map[grid_id]["cloud"] > cloud: # replace with lower cloud
                target_tiles_map[grid_id] = {
                    "grid_id": grid_id,
                    "cloud": cloud,
                    "file_path": fname,
                }

    print('总耗时：',time.time()-start_time)
    
    
    
# Step 3 : 合并所有影像瓦片
pathes = [target_tiles_map[grid_id]['file_path'] for grid_id in target_tiles_map]
print(pathes)
xfer.toolbox.merge_tiles(pathes, './temp/merged.tif')
