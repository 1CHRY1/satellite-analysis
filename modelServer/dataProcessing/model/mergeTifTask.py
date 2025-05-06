import os
import uuid
from datetime import datetime


from dataProcessing.Utils.mySqlUtils import select_tile_by_column_and_row
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mtif, mband, convert_tif2cog
from dataProcessing.model.task import Task
import dataProcessing.config as config

MINIO_ENDPOINT = f"{config.MINIO_IP}:{config.MINIO_PORT}"


class MergeTifTask(Task):

    def run(self):
        # --------- Extract the request info ---------------------------
        tiles = self.args[0].get('tiles', [])
        bands = self.args[0].get('bands', [])
        sceneId = self.args[0].get('sceneId', "")
        if not tiles:
            return "No IDs provided", 400
        if isinstance(tiles, dict):
            tiles = [tiles]  # 转换为列表

        # --------- Get Source Data ------------------------------------
        tile_list = select_tile_by_column_and_row(sceneId.lower(), tiles, bands)
        merged_tif_list = []
        for band, tiles in tile_list.items():
            tif_paths = [f"http://{MINIO_ENDPOINT}/{tile['bucket']}/{tile['path']}" for tile in tiles]
            temp_tif_path = os.path.join(config.TEMP_OUTPUT_DIR, f"{uuid.uuid4()}.tif")
            mtif(tif_paths, temp_tif_path)
            merged_tif_list.append({"path": temp_tif_path, "band": band})

        # --------- Merge and upload tif -------------------------------
        output_file_path = mband(merged_tif_list, config.TEMP_OUTPUT_DIR, f"{uuid.uuid4()}.tif")
        output_file_path = convert_tif2cog(output_file_path)
        object_name = f"{datetime.now().strftime('%Y-%m/%d')}/{uuid.uuid4()}.tif"
        uploadLocalFile(output_file_path, config.MINIO_TEMP_FILES_BUCKET, object_name)
        # time.sleep(20)
        return {
            "bucket": config.MINIO_TEMP_FILES_BUCKET,
            "path": object_name
        }
