import json, os, time, math

import numpy as np

from shapely.geometry import shape, box

from rio_tiler.io import COGReader
from rasterio.transform import from_bounds
import rasterio

from dataProcessing.model.task import Task
import dataProcessing.config as config
from dataProcessing.Utils.gridUtil import GridHelper

MINIO_ENDPOINT = f"http://{config.MINIO_IP}:{config.MINIO_PORT}"
INFINITY = 999999

#### Main #############################################################

class calc_no_cloud(Task):
    
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        self.tiles = self.args[0].get('tiles', [])
        self.resolution = self.args[0].get('resolution', 1)
        self.cloud = self.args[0].get('cloud', 10)
        self.scenes = self.args[0].get('scenes', [])

    def run(self):
        print("MinCloudGraphTask run")

        ## Step 1 : Input Args #################################################
        grids = self.tiles
        gridResolution = self.resolution
        scenes = self.scenes
        # cloud = data.get('cloud') 没啥用



        ## Step 2 : Grids  ##############################################
        grid_helper = GridHelper(gridResolution)


        ## Main Helper 

        grid_bbox_cache_map = {} # as cache
        cloud_band_context_map = {} # as cache
        red_band_context_map = {} # as cache 
        green_band_context_map = {} # as cache 
        blue_band_context_map = {} # as cache 

        def grid_bbox(grid_x, grid_y):
            
            key = f'{grid_x}_{grid_y}'
            
            if key in grid_bbox_cache_map:
                return grid_bbox_cache_map[key]
            else:
                bbox = GridHelper._get_grid_polygon(grid_x, grid_y)
                grid_bbox_cache_map[key] = bbox
                return bbox.bounds


        def is_grid_covered(scene, grid_x, grid_y):

            bbox_wgs84 = grid_bbox(grid_x, grid_y) # [ L B R T ]
            scene_geom = scene.get('bbox').get('geometry')
            
            polygon = shape(scene_geom)
            bbox_polygon = box(*bbox_wgs84)
            
            return polygon.contains(bbox_polygon)


        def calc_grid_cloud_CloudBand(scene, grid_x, grid_y):
            '''
            从cloud_band_context_map取出打开的上下文来读像素, 返回mask
            '''
            
            ctx: COGReader = cloud_band_context_map.get(scene.get('sceneId'))
            bbox_wgs84 = grid_bbox(grid_x, grid_y)
            
            img_data = ctx.part(bbox=bbox_wgs84, indexes=[1])
            image_data = img_data.data[0]
            

            nodata_mask = img_data.mask  # shape: (H, W), dtype=uint8 or bool
                
            sensorName = scene.get('sensorName')
        
            if 'Landsat' in sensorName:
                cloud_mask = (image_data & (1 << 3)) > 0  # 提取第3位
            if 'Landset' in sensorName:
                cloud_mask = (image_data & (1 << 3)) > 0  # 提取第3位
            elif 'MODIS' in sensorName:
                cloud_mask = (image_data & 1) > 0  # 提取第0位
            elif "GF" in sensorName:
                cloud_mask = (image_data == 2)
            else:
                raise NotImplementedError(f"Cloud logic not implemented for sensor: {sensorName}")
            
            nodata = nodata_mask.astype(bool)
            cloud = cloud_mask.astype(bool)
            valid_mask = (~nodata) & (~cloud)
            
            return {
                'nodata': nodata,
                'cloud': cloud,
                'valid_mask': valid_mask,
                'cloud': np.sum(valid_mask) / np.sum(image_data) * 100
            }


        def calc_grid_cloud_SceneCloud(scene, grid_x, grid_y):
            
            # 直接返回景的云量
            return {
                'cloud': scene.get('cloud')
            }
            

        def calc_grid_cloud(scene, grid_x, grid_y):

            cloud_band_path = scene.get('cloudPath')
            opened_context_in_map = cloud_band_context_map.get(scene.get('sceneId'))
            
            if(cloud_band_path != None):
                if(opened_context_in_map != None):
                    return calc_grid_cloud_CloudBand(scene, grid_x, grid_y)
                else:
                    full_url = MINIO_ENDPOINT + "/" + scene.get('bucket') + '/' + cloud_band_path
                    opened_context = COGReader(full_url)
                    cloud_band_context_map[scene.get('sceneId')] = opened_context
                    return calc_grid_cloud_CloudBand(scene, grid_x, grid_y)

            return calc_grid_cloud_SceneCloud(scene, grid_x, grid_y)



        # 预处理
        scene_band_paths = {} # as cache 
        for scene in scenes:
            mapper = scene['bandMapper']
            bands = { 'red': None, 'green': None, 'blue': None }
            for img in scene['images']:
                if img['band'] == mapper['Red']:
                    bands['red'] = img['tifPath']
                if img['band'] == mapper['Green']:
                    bands['green'] = img['tifPath']
                if img['band'] == mapper['Blue']:
                    bands['blue'] = img['tifPath']
            scene_band_paths[scene['sceneId']] = bands



        # O(m * n) traverse
        grid_result_collection = []
        for grid in grids:
            
            [grid_x, grid_y] = grid
            
            min_cloud_of_grid = INFINITY
            target_scene_of_grid = None
            
            for scene in scenes:
            
                # Open cloud-band
                cloud_result = calc_grid_cloud(scene, grid_x, grid_y)
                
                if(cloud_result.get('cloud') == 0): # 0云量 jump
                    target_scene_of_grid = scene
                    break
                
                elif cloud_result.get('cloud') < min_cloud_of_grid: # cloud jump
                    min_cloud_of_grid = cloud_result.get('cloud')
                    target_scene_of_grid = scene
            
            grid_result_collection.append({
                'grid_x': grid_x,
                'grid_y': grid_y,
                'cloud': min_cloud_of_grid,
                'scene': target_scene_of_grid,
            })
                

        for key, value in cloud_band_context_map.items():
            
            cloud_band_context_map.get(key).close() # relase context


        # O(m) travers

        grid_tiles = {}  # {(grid_x, grid_y): (R, G, B)}

        minx, miny, maxx, maxy = INFINITY, INFINITY, -INFINITY, -INFINITY

        for index, grid_res in enumerate(grid_result_collection):
            
            target_scene = grid_res.get('scene')
            grid_x, grid_y = grid_res.get('grid_x'), grid_res.get('grid_y') 
            gridbbox = grid_helper._get_grid_polygon(grid_x, grid_y)
                
            # update global bounds
            minx = min(minx, gridbbox[0])
            miny = min(miny, gridbbox[1])
            maxx = max(maxx, gridbbox[2])
            maxy = max(maxy, gridbbox[3])    
            
            #######################################################

            paths = scene_band_paths[target_scene['sceneId']]
            red_path = paths['red']
            green_path = paths['green']
            blue_path = paths['blue']

            
            red_reader = red_band_context_map.get(red_path)
            if red_reader is None:
                red_band_full_path = MINIO_ENDPOINT + "/" + target_scene.get('bucket') + '/' + red_path
                red_reader = COGReader(red_band_full_path)
                print('new', red_band_full_path)
                red_band_context_map[red_path] = red_reader
            
            green_reader = green_band_context_map.get(green_path)
            if green_reader is None:
                green_band_full_path = MINIO_ENDPOINT + "/" + target_scene.get('bucket') + '/' + green_path
                green_reader = COGReader(green_band_full_path)
                green_band_context_map[green_path] = green_reader
            
            blue_reader = blue_band_context_map.get(blue_path)
            if blue_reader is None:
                blue_band_full_path = MINIO_ENDPOINT + "/" + target_scene.get('bucket') + '/' + blue_path
                blue_reader = COGReader(blue_band_full_path)
                blue_band_context_map[blue_path] = blue_reader
                
            R = red_reader.part(bbox=gridbbox, indexes=[1]).data[0]
            
            G = green_reader.part(bbox=gridbbox, indexes=[1]).data[0]
            
            B = blue_reader.part(bbox=gridbbox, indexes=[1]).data[0]
            
            grid_tiles[(grid_x, grid_y)] = np.stack([R, G, B], axis=0)  # shape: (3, H, W)
            
            print("processing ", index + 1, " / ", len(grid_result_collection))
                    
            #######################################################

        for key, value in red_band_context_map.items():
            
            red_band_context_map.get(key).close() # relase context

        for key, value in green_band_context_map.items():
            
            green_band_context_map.get(key).close()
            
        for key, value in blue_band_context_map.items():
            
            blue_band_context_map.get(key).close()


        grid_xs = sorted(set(x for x, y in grid_tiles.keys()))
        grid_ys = sorted(set(y for x, y in grid_tiles.keys()))

        example_tile = next(iter(grid_tiles.values()))
        tile_shape = example_tile.shape  # e.g. (3, 256, 256)
        empty_tile = np.zeros(tile_shape, dtype=example_tile.dtype)

        rows = []
        for y in grid_ys:
            row_tiles = []
            for x in grid_xs:
                tile = grid_tiles.get((x, y), empty_tile)
                if tile is None: continue
                row_tiles.append(tile)
            row_concat = np.concatenate(row_tiles, axis=2)
            rows.append(row_concat)


        final_image = np.concatenate(rows, axis=1)

        # 写入
        transform = from_bounds(minx, miny, maxx, maxy, final_image.shape[2], final_image.shape[1])

        with rasterio.open(
            "MERGE_COG.tif", 'w',
            driver='COG',
            height=final_image.shape[1],
            width=final_image.shape[2],
            count=3,
            dtype=final_image.dtype,
            crs='EPSG:4326',
            transform=transform
        ) as dst:
            dst.write(final_image)

        return {
            "bucket": "no",
            "tifPath": "MERGE_COG.tif"
        }
            

