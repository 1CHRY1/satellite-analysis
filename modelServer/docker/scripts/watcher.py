#!/usr/bin/env python3
"""
Ray优化环境的项目监控脚本
监控文件变化，管理Ray集群，提供项目支持功能
"""

import os
import sys
import time
import json
import logging
import threading
import signal
from datetime import datetime
from pathlib import Path
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('/usr/local/coding/watcher.log')
    ]
)
logger = logging.getLogger('RayWatcher')

class RayProjectHandler(FileSystemEventHandler):
    """处理项目文件变化的事件处理器"""
    
    def __init__(self):
        super().__init__()
        self.ray_initialized = False
        self.init_ray()
    
    def init_ray(self):
        """初始化Ray集群"""
        try:
            import ray
            
            # 尝试连接现有集群
            try:
                ray.init(address='auto', ignore_reinit_error=True)
                logger.info("已连接到现有Ray集群")
            except:
                # 启动本地Ray节点
                ray.init(
                    num_cpus=4,
                    object_store_memory=1024*1024*1024,  # 1GB
                    ignore_reinit_error=True
                )
                logger.info("已启动本地Ray节点")
            
            # 显示集群信息
            cluster_resources = ray.cluster_resources()
            logger.info(f"Ray集群资源: {cluster_resources}")
            self.ray_initialized = True
            
        except Exception as e:
            logger.error(f"Ray初始化失败: {e}")
            self.ray_initialized = False
    
    def on_created(self, event):
        """文件创建事件"""
        if not event.is_directory:
            logger.info(f"新文件创建: {event.src_path}")
            self._handle_python_file(event.src_path)
    
    def on_modified(self, event):
        """文件修改事件"""
        if not event.is_directory:
            logger.info(f"文件修改: {event.src_path}")
            self._handle_python_file(event.src_path)
    
    def _handle_python_file(self, filepath):
        """处理Python文件变化"""
        if filepath.endswith('.py') and 'main.py' in filepath:
            logger.info(f"检测到主Python文件变化: {filepath}")
            
            # 检查是否需要Ray优化
            if self._should_optimize(filepath):
                logger.info(f"文件 {filepath} 适合Ray优化")
            else:
                logger.info(f"文件 {filepath} 不需要特殊优化")
    
    def _should_optimize(self, filepath):
        """判断文件是否需要优化"""
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 简单检测是否包含可优化的模式
            optimization_keywords = [
                'for ', 'range(', 'map(', 'filter(',
                'numpy', 'pandas', 'cv2', 'PIL',
                'glob.glob', 'os.listdir'
            ]
            
            return any(keyword in content for keyword in optimization_keywords)
            
        except Exception as e:
            logger.error(f"分析文件失败: {e}")
            return False


class RayProjectManager:
    """Ray项目管理器"""
    
    def __init__(self, project_dir="/usr/local/coding"):
        self.project_dir = Path(project_dir)
        self.data_dir = self.project_dir / "data"
        self.output_dir = self.project_dir / "output"
        self.temp_dir = self.project_dir / "temp"
        
        # 确保目录存在
        for dir_path in [self.data_dir, self.output_dir, self.temp_dir]:
            dir_path.mkdir(exist_ok=True)
        
        # 初始化监控
        self.observer = Observer()
        self.handler = RayProjectHandler()
        
        # 设置信号处理
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
        
        logger.info(f"Ray项目管理器初始化完成，项目目录: {self.project_dir}")
    
    def start_monitoring(self):
        """开始监控项目目录"""
        self.observer.schedule(
            self.handler, 
            str(self.project_dir), 
            recursive=True
        )
        self.observer.start()
        logger.info("开始监控项目文件变化")
        
        try:
            # 运行状态报告线程
            status_thread = threading.Thread(target=self._status_reporter, daemon=True)
            status_thread.start()
            
            # 主循环
            while True:
                time.sleep(1)
                
        except KeyboardInterrupt:
            logger.info("收到中断信号，正在停止监控...")
        finally:
            self.stop_monitoring()
    
    def stop_monitoring(self):
        """停止监控"""
        if self.observer.is_alive():
            self.observer.stop()
            self.observer.join()
        
        # 清理Ray资源
        try:
            import ray
            if ray.is_initialized():
                ray.shutdown()
                logger.info("Ray集群已关闭")
        except:
            pass
        
        logger.info("项目监控已停止")
    
    def _status_reporter(self):
        """定期报告系统状态"""
        while True:
            try:
                # 收集系统信息
                status = {
                    'timestamp': datetime.now().isoformat(),
                    'project_dir': str(self.project_dir),
                    'ray_status': self._get_ray_status(),
                    'disk_usage': self._get_disk_usage(),
                    'process_info': self._get_process_info()
                }
                
                # 保存状态到文件
                status_file = self.project_dir / "status.json"
                with open(status_file, 'w', encoding='utf-8') as f:
                    json.dump(status, f, indent=2, ensure_ascii=False)
                
                time.sleep(30)  # 每30秒更新一次状态
                
            except Exception as e:
                logger.error(f"状态报告失败: {e}")
                time.sleep(60)
    
    def _get_ray_status(self):
        """获取Ray集群状态"""
        try:
            import ray
            if ray.is_initialized():
                return {
                    'initialized': True,
                    'cluster_resources': ray.cluster_resources(),
                    'available_resources': ray.available_resources()
                }
            else:
                return {'initialized': False}
        except:
            return {'initialized': False, 'error': 'Ray not available'}
    
    def _get_disk_usage(self):
        """获取磁盘使用情况"""
        try:
            import shutil
            total, used, free = shutil.disk_usage(self.project_dir)
            return {
                'total_gb': round(total / (1024**3), 2),
                'used_gb': round(used / (1024**3), 2),
                'free_gb': round(free / (1024**3), 2),
                'usage_percent': round((used / total) * 100, 2)
            }
        except:
            return {'error': 'Cannot get disk usage'}
    
    def _get_process_info(self):
        """获取进程信息"""
        try:
            import psutil
            process = psutil.Process()
            return {
                'pid': process.pid,
                'cpu_percent': process.cpu_percent(),
                'memory_mb': round(process.memory_info().rss / (1024**2), 2),
                'create_time': datetime.fromtimestamp(process.create_time()).isoformat()
            }
        except:
            return {'error': 'Cannot get process info'}
    
    def _signal_handler(self, signum, frame):
        """信号处理器"""
        logger.info(f"收到信号 {signum}，正在优雅关闭...")
        self.stop_monitoring()
        sys.exit(0)


def main():
    """主函数"""
    logger.info("=" * 50)
    logger.info("Ray优化Python环境监控器启动")
    logger.info("=" * 50)
    
    # 显示环境信息
    logger.info(f"Python版本: {sys.version}")
    logger.info(f"工作目录: {os.getcwd()}")
    
    try:
        import ray
        logger.info(f"Ray版本: {ray.__version__}")
    except ImportError:
        logger.error("Ray未安装，某些功能可能不可用")
    
    # 创建并启动项目管理器
    manager = RayProjectManager()
    
    try:
        manager.start_monitoring()
    except Exception as e:
        logger.error(f"监控启动失败: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main() 