from ogms_xfer import OGMS_Xfer as xfer
from datetime import datetime

config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################
# 1. 检索影像 用ID检索
scene = xfer.Scene("SC906772444") # 直接通过ID检索

if(scene.scene_id == None):
    print("无法检索到影像, 请检查影像ID是否正确")
    exit()


print("检索到影像： ", scene.scene_name)
print("波段数： ",scene.band_num)
print("波段列表： ",scene.bands)

# 2. 取影像红光和近红外波段URL， 传入计算NDVI
nir_url = xfer.URL.resolve(scene.get_band_image(5).url)
red_url = xfer.URL.resolve(scene.get_band_image(4).url)
output_path = xfer.URL.outputUrl('ndvibyremote.tif')

start_time = datetime.datetime.now()
xfer.ModelStore.ndvi(nir_url, red_url, output_path)
end_time = datetime.datetime.now()
print(f"云端资源计算NDVI总时间: {end_time - start_time}")
print(' --------------------------------- ')