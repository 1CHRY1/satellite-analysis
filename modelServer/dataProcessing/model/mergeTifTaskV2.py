import os
import uuid
from datetime import datetime


from dataProcessing.Utils.mySqlUtils import select_tile_by_column_and_row, select_tile_by_column_and_row_v2
from dataProcessing.Utils.osUtils import uploadLocalFile
from dataProcessing.Utils.tifUtils import mtif, mband, convert_tif2cog, mtif_v2
from dataProcessing.model.task import Task
from dataProcessing.config import current_config as CONFIG

MINIO_ENDPOINT = f"{CONFIG.MINIO_IP}:{CONFIG.MINIO_PORT}"


class MergeTifTaskV2(Task):

    def run(self):
        print("MergeTifTaskV2 run")
        # --------- Extract the request info ---------------------------
        tiles = self.args[0].get('tiles', [])
        bands = self.args[0].get('bands', [])
        print("bands:", bands)
        if not tiles:
            return "No IDs provided", 400
        if isinstance(tiles, dict):
            tiles = [tiles]  # 转换为列表

        # --------- Get Source Data ------------------------------------
        tile_list = select_tile_by_column_and_row_v2(tiles, bands)
        merged_tif_list = []
        for band, tiles in tile_list.items():
            tif_paths = [f"http://{MINIO_ENDPOINT}/{tile['bucket']}/{tile['path']}" for tile in tiles]
            temp_tif_path = os.path.join(CONFIG.TEMP_OUTPUT_DIR, f"{uuid.uuid4()}.tif")
            # mtif(tif_paths, temp_tif_path)
            mtif_v2(tif_paths, temp_tif_path)
            merged_tif_list.append({"path": temp_tif_path, "band": band})

        # --------- Merge and upload tif -------------------------------
        # mband 变化，以下逻辑暂时不可用
        # output_file_path = mband(merged_tif_list, CONFIG.TEMP_OUTPUT_DIR, f"{uuid.uuid4()}.tif")
        # output_file_path = convert_tif2cog(output_file_path)
        object_name = f"{datetime.now().strftime('%Y-%m/%d')}/{uuid.uuid4()}.tif"
        # uploadLocalFile(output_file_path, CONFIG.MINIO_TEMP_FILES_BUCKET, object_name)
        # time.sleep(20)
        return {
            "bucket": CONFIG.MINIO_TEMP_FILES_BUCKET,
            "path": object_name
        }
