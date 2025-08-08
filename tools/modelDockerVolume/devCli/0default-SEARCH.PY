from ogms_xfer import OGMS_Xfer as xfer
config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################
from datetime import datetime
import json

def preview_info(scene):
    print("-"*50)
    print("影像ID：",scene.scene_id)
    print("影像名称：",scene.scene_name)
    print("影像所属产品：",scene.product_id)
    print("影像时间：",scene.scene_time)
    print("影像云量：",scene.cloud)
    print("影像坐标系：",scene.coordinate_system)
    print("影像波段：",scene.bands)
    print("-"*50)

# # 1. 检索影像 用ID检索
# print("用ID检索")
# scene = xfer.Scene("SC04u521n84")
# preview_info(scene)

# # 2. 检索影像 用影像名匹配检索
# print("用影像名匹配检索")
# scene = xfer.Scene().query(scene_name="GF1D_PMS_E116.9_N36.9_20250416_L1A1257591987_beauty")[0]
# preview_info(scene)

# # 3. 检索影像 用影像产品匹配检索
# print("用影像产品匹配检索")
# scenes = xfer.Scene().query(product_id="P77476176")
# preview_info(scenes[0])

# # 4. 检索影像，用时间范围检索
# print("用时间范围检索")
# scenes = xfer.Scene().query(time_range= [datetime(2023, 1, 1), datetime(2025, 5, 31)])
# for scene in scenes:
#     preview_info(scene)

# # 5. 检索影像，用空间范围检索
# print("用空间范围检索")
# with open(xfer.URL.dataUrl('region.geojson'), "r") as f:
#     region = json.load(f)
#     scenes = xfer.Scene().query(polygon=region)
#     print("检索到",len(scenes),"景影像")
#     for scene in scenes:
#         preview_info(scene)

# 6. 检索影像，用云量范围检索
print("用云量范围检索")
scenes = xfer.Scene().query(cloud_range= [0, 1])
for scene in scenes:
    preview_info(scene)