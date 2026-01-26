import os
import sys
import json
import math
import uuid
import time
import shutil
import tempfile
import logging
import numpy as np
import rasterio
from rasterio.windows import from_bounds
from rasterio.transform import from_origin
from concurrent.futures import ThreadPoolExecutor, as_completed
from shapely import wkt
from flask import Flask, request, Response, jsonify
from dataProcessing.config import current_config as CONFIG

# 确保导入 Task 父类
from dataProcessing.model.task import Task

# 引入 GDAL 用于构建 VRT
from osgeo import gdal

# 引入 rio-tiler 用于动态切片服务
from rio_tiler.io import Reader
from rio_tiler.profiles import img_profiles
from rio_tiler.errors import TileOutsideBounds

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ESRGANEngine")

# =================配置部分=================

# 1. 确定缓存目录
os.makedirs(CONFIG.CACHE_ROOT, exist_ok=True)

# 2. 外部算法接口地址ESRGAN_MODEL_URL

# =================核心处理类=================

class esrgan(Task):
    def __init__(self, task_id, *args, **kwargs):
        super().__init__(task_id, *args, **kwargs)
        
        data = self.args[0] if self.args else {}
        
        self.task_id = task_id
        self.band = data.get('band', {})
        self.tiles = data.get('tiles', [])
        self.resolution = data.get('resolution', 1)
        self.boundary = data.get('boundary', None)
        
        self.task_dir = os.path.join(CONFIG.CACHE_ROOT, self.task_id)
        os.makedirs(self.task_dir, exist_ok=True)
        
        self.tile_size = 1024

    @staticmethod
    def convert_to_uint8(data, original_dtype):
        """
        自动将不同数据类型转换为uint8
        """
        if original_dtype == np.uint8:
            return data.astype(np.uint8)
        elif original_dtype == np.uint16:
            # 将 uint16 (0-65535) 线性映射到 uint8 (0-255)
            # 注意：这里直接除以65535可能会导致图像过暗，
            # 实际遥感影像通常只用了低位数据，但在你的逻辑里保持严谨即可
            return (data / 65535.0 * 255.0).astype(np.uint8)
        elif original_dtype == np.float32 or original_dtype == float:
            data_min = np.min(data)
            data_max = np.max(data)
            if data_min >= 0 and data_max <= 255:
                return data.astype(np.uint8)
            elif data_min >= 0 and data_max <= 65535:
                temp_uint16 = data.astype(np.uint16)
                return (temp_uint16 / 65535.0 * 255.0).astype(np.uint8)
            else:
                # 归一化映射到0-255
                if data_max > data_min:
                    normalized = (data - data_min) / (data_max - data_min)
                    return (normalized * 255.0).astype(np.uint8)
                else:
                    return np.zeros_like(data, dtype=np.uint8)
        else:
            # 其他类型，先归一化到0-1，再映射to 0-255
            data_min = np.min(data)
            data_max = np.max(data)
            if data_max > data_min:
                normalized = (data - data_min) / (data_max - data_min)
                return (normalized * 255.0).astype(np.uint8)
            else:
                return np.zeros_like(data, dtype=np.uint8)
        
    def _get_bbox_from_wkt(self, wkt_str):
        if not wkt_str:
            raise ValueError("Boundary WKT is missing")
        return wkt.loads(wkt_str).bounds

    def _cleanup_old_tasks(self):
        """
        1. 优先保证当前磁盘/内存有足够的剩余空间。
        2. 只有当空间不足时，才按时间顺序删除最旧的任务。
        3. 永远不删除当前任务。
        """
        # ================= 配置区 =================
        # 设定最小剩余空间 (单位: 字节)，例如预留 2GB
        # 如果你使用的是 /dev/shm (内存)，这个值要根据你的物理内存大小谨慎设置
        MIN_FREE_BYTES = CONFIG.MIN_FREE_BYTES
        
        # 设定任务的“绝对过期时间” (秒)，例如 1 小时前的任务无论空间够不够都删
        MAX_TTL_SECONDS = 3600
        # =========================================

        try:
            logger.info("Starting cleanup...")
            
            # 1. 获取所有任务文件夹的信息
            tasks_info = []
            now = time.time()
            
            for item in os.listdir(CONFIG.CACHE_ROOT):
                item_path = os.path.join(CONFIG.CACHE_ROOT, item)
                
                # 跳过非文件夹
                if not os.path.isdir(item_path):
                    continue
                
                # 绝对不能删除当前正在运行的任务
                if item == self.task_id:
                    continue
                    
                try:
                    # 获取文件夹的创建/修改时间
                    mtime = os.path.getmtime(item_path)
                    tasks_info.append({
                        "path": item_path,
                        "id": item,
                        "time": mtime,
                        "age": now - mtime
                    })
                except OSError:
                    continue

            # 2. 按时间排序：最旧的在前面 (升序)
            tasks_info.sort(key=lambda x: x["time"])

            # 3. 阶段一：强制删除超时的任务 (TTL)
            remaining_tasks = []
            for task in tasks_info:
                if task["age"] > MAX_TTL_SECONDS:
                    try:
                        shutil.rmtree(task["path"], ignore_errors=True)
                        logger.info(f"Removed expired task (Age: {int(task['age'])}s): {task['id']}")
                    except Exception as e:
                        logger.warning(f"Failed to remove {task['id']}: {e}")
                else:
                    remaining_tasks.append(task)

            # 4. 阶段二：空间不足时，LRU 淘汰 (删最旧的)
            # 检查所在磁盘的剩余空间
            total, used, free = shutil.disk_usage(CONFIG.CACHE_ROOT)
            
            # 如果剩余空间 < 设定阈值，开始循环删除，直到空间足够 或 删无可删
            deleted_count = 0
            while free < MIN_FREE_BYTES and remaining_tasks:
                # 取出最旧的一个
                task_to_remove = remaining_tasks.pop(0)
                try:
                    shutil.rmtree(task_to_remove["path"], ignore_errors=True)
                    logger.info(f"Low Space ({free//1024//1024}MB free). Removed old task: {task_to_remove['id']}")
                    deleted_count += 1
                    
                    # 重新检查空间 (虽然频繁IO，但在清理阶段是安全的)
                    total, used, free = shutil.disk_usage(CONFIG.CACHE_ROOT)
                except Exception as e:
                    logger.warning(f"Failed to remove {task_to_remove['id']}: {e}")
            
            logger.info(f"Cleanup done. Free Space: {free//1024//1024} MB")

        except Exception as e:
            logger.error(f"Cleanup process failed: {e}")

    def _read_and_stack(self, bbox):
        """按范围读取 RGBA/NIR 并堆叠，并在最后统一转 uint8"""
        # 引入坐标转换工具
        from rasterio.warp import transform_bounds
        
        band_keys = ['B', 'G', 'R']
        paths = [self.band.get(k) for k in band_keys]
        
        nir_path = self.band.get('NIR') or self.band.get('N')
        if nir_path:
            paths.append(nir_path)
            
        logger.info(f"Reading bands from: {paths}")
        
        src_datasets = []
        try:
            valid_paths = [p for p in paths if p]
            if not valid_paths:
                raise ValueError("No valid band paths provided")
                
            ref_src = rasterio.open(valid_paths[0])
            src_datasets.append(ref_src)
            
            # ================= 修复开始：坐标系对齐 =================
            # 1. 检查图片坐标系
            src_crs = ref_src.crs
            
            # 2. 如果图片有坐标系，且不是经纬度 (EPSG:4326)，则需要投影转换
            # 你的 bbox 默认是 EPSG:4326 (经纬度)
            if src_crs and src_crs != 'EPSG:4326':
                left, bottom, right, top = bbox
                # 将 bbox 从 4326 转到 图片的坐标系 (例如 EPSG:32651)
                new_left, new_bottom, new_right, new_top = transform_bounds(
                    'EPSG:4326', src_crs, left, bottom, right, top
                )
                logger.info(f"Transformed BBox from 4326 to {src_crs}: {new_left}, {new_bottom}, {new_right}, {new_top}")
                # 使用转换后的坐标计算窗口
                window = from_bounds(new_left, new_bottom, new_right, new_top, transform=ref_src.transform)
                
                # 更新用于后续计算 Transform 的 bbox 变量，确保后续生成的 tif 坐标正确
                calc_bbox = (new_left, new_bottom, new_right, new_top)
            else:
                # 如果图片本身就是经纬度，或者没坐标系，直接用
                window = from_bounds(*bbox, transform=ref_src.transform)
                calc_bbox = bbox
            # ================= 修复结束 =================

            window = window.round_offsets().intersection(
                rasterio.windows.Window(0, 0, ref_src.width, ref_src.height)
            )
            
            # 获取这个窗口对应的局部 Transform
            window_transform = ref_src.window_transform(window)
            
            data_list = []
            
            # 遍历读取所有波段
            for p in paths:
                if p:
                    if p == valid_paths[0]:
                         src = ref_src
                    else:
                         src = rasterio.open(p)
                         src_datasets.append(src)
                    
                    # 现在 window 是基于图片坐标系计算的，绝对准确
                    # 并且 boundless=True 在这里依然有效
                    data_list.append(src.read(1, window=window, boundless=True, fill_value=0))
                else:
                    pass
            
            # 1. 堆叠原始数据
            stack = np.stack(data_list)
            
            # 2. 转 uint8
            original_dtype = stack.dtype
            logger.info(f"Converting stack from {original_dtype} to uint8...")
            stack = self.convert_to_uint8(stack, original_dtype)
            
            # 3. 计算切片后的 Transform (必须用转换后的坐标 calc_bbox)
            c, h, w = stack.shape
            minx, miny, maxx, maxy = calc_bbox
            new_transform = rasterio.transform.from_bounds(minx, miny, maxx, maxy, w, h)

            return stack, new_transform, ref_src.crs, ref_src.profile
            
        finally:
            for s in src_datasets: s.close()

    def _process_tile(self, tile_data, tile_transform, idx, crs, profile_template):
        """处理单块"""
        try:
            c, h, w = tile_data.shape
            
            # 1. 准备 Profile
            prof = profile_template.copy()
            # 强制更新 dtype 为 uint8，因为传入的数据已经是 uint8 了
            prof.update({
                'driver': 'GTiff', 
                'height': h, 
                'width': w, 
                'count': c,
                'dtype': 'uint8',  # 明确指定
                'transform': tile_transform,
                'crs': crs, 
                'nodata': 0, 
                'compress': 'lzw'
            })
            
            # 2. 内存编码 TIF
            mem_bytes = None
            with rasterio.MemoryFile() as memfile:
                with memfile.open(**prof) as dst:
                    dst.write(tile_data)
                mem_bytes = memfile.read()

            # 3. 模拟调用外部接口 (Request)
            import requests
            res = requests.post(CONFIG.ESRGAN_MODEL_URL, data=mem_bytes, timeout=6000)
            if res.status_code != 200:
                logger.error(f"ESRGAN API Error: {res.text}")
                return None
            processed_bytes = res.content

            # Mock版
            # processed_bytes = mem_bytes

            # 4. 写入共享内存目录
            tile_name = f"tile_{idx[0]}_{idx[1]}.tif"
            save_path = os.path.join(self.task_dir, tile_name)
            
            with open(save_path, 'wb') as f:
                f.write(processed_bytes)
                
            return save_path

        except Exception as e:
            logger.error(f"Tile {idx} failed: {e}")
            return None

    def run(self):
        logger.info(f"Starting Task {self.task_id}")
        start_time = time.time()
        
        try:
            # 1. 读取数据 (这里返回的 img_data 已经是 uint8 了)
            bbox = self._get_bbox_from_wkt(self.boundary)
            img_data, base_transform, crs, base_profile = self._read_and_stack(bbox)
            
            _, full_h, full_w = img_data.shape
            logger.info(f"Loaded Data Shape (uint8): {img_data.shape}")

            # 2. 并行切片策略
            futures = []
            n_rows = math.ceil(full_h / self.tile_size)
            n_cols = math.ceil(full_w / self.tile_size)
            
            logger.info(f"Grid Strategy: {n_rows} rows x {n_cols} cols")
            
            with ThreadPoolExecutor(max_workers=os.cpu_count()) as executor:
                for r_idx in range(n_rows):
                    for c_idx in range(n_cols):
                        y = r_idx * self.tile_size
                        x = c_idx * self.tile_size

                        # ----- 反选逻辑开始 ------
                        if x + self.tile_size > full_w:
                            if full_w >= self.tile_size:
                                x = full_w - self.tile_size
                        
                        # 同样逻辑处理高度 y
                        if y + self.tile_size > full_h:
                            if full_h >= self.tile_size:
                                y = full_h - self.tile_size

                        # ------------------------
                        
                        tile = img_data[:, y:y+self.tile_size, x:x+self.tile_size]

                        # Padding 逻辑
                        d_c, d_h, d_w = tile.shape
                        if d_h < self.tile_size or d_w < self.tile_size:
                            pad_h = self.tile_size - d_h
                            pad_w = self.tile_size - d_w
                            tile = np.pad(tile, 
                                          ((0,0), (0, pad_h), (0, pad_w)), 
                                          mode='constant', 
                                          constant_values=0)
                        
                        tile_transform = base_transform * rasterio.Affine.translation(x, y)
                        
                        futures.append(executor.submit(
                            self._process_tile,
                            tile, tile_transform, (r_idx, c_idx), crs, base_profile
                        ))
            
            # 3. 收集结果
            processed_files = [f.result() for f in as_completed(futures) if f.result()]
            
            if not processed_files:
                raise RuntimeError("No tiles processed")

            # 4. 生成 VRT
            vrt_path = os.path.join(self.task_dir, "index.vrt")
            options = gdal.BuildVRTOptions(resampleAlg='nearest', addAlpha=True)
            gdal.BuildVRT(vrt_path, processed_files, options=options)
            
            logger.info(f"VRT generated at {vrt_path} in {time.time()-start_time:.2f}s")
            self._cleanup_old_tasks()
            
            return {
                "task_id": self.task_id,
                "vrt_path": vrt_path
            }

        except Exception as e:
            logger.error(f"Task Failed: {e}", exc_info=True)
            shutil.rmtree(self.task_dir, ignore_errors=True)
            raise