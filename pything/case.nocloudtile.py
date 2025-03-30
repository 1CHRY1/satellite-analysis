from ogms_xfer import OGMS_Xfer as xfer
import json, os

data_dir = os.path.join(os.path.dirname(__file__), "data")
output_dir = os.path.join(os.path.dirname(__file__), "output")

if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    xfer.initialize(config_file_path)
    
    scene = xfer.Scene("SC906772444")
    image = scene.get_band_image("1")
    
    with open(os.path.join(data_dir, "region.geojson"), "r") as f:
        region = json.load(f)

        region_tiles = image.get_tiles_by_polygon(region)
    
        print(1)
    
        # no_cloud_tiles = image.get_tiles(
        #     polygon=region,
        #     max_cloud=0.001
        # )
