# 这个类应该和其他不太一样，主要是结果数据的存储和下载
from service.project_data import ProjectDataService
from application.provider import Singleton

class ProjectDataTransfer:

    @classmethod
    def download(cls, data_id: str, output_path: str):
        # 从项目资产中下载数据
        from service.project_data import ProjectDataService
        project_data_service : ProjectDataService = Singleton.get_instance(id="project_data_service")
        project_data_service.pull_data(data_id, output_path)
        
    @classmethod
    def upload(cls, data_name: str, data_type: str, file_path: str):
        # 上传数据至项目资产
        from service.project_data import ProjectDataService
        project_data_service : ProjectDataService = Singleton.get_instance(id="project_data_service")
        project_data_item = project_data_service.create_project_data(
            data_name = data_name,
            project_id = Singleton.get_value(id="project_id"),
            user_id = Singleton.get_value(id="user_id"),
            bucket = Singleton.get_value(id="project_bucket"),
            data_type = data_type
        )
        project_data_service.push_data(project_data_item, file_path)
        
    @classmethod
    def upload_from_bytes(cls, data_name: str, data_type: str, data: bytes, length: int):
        # 上传数据至项目资产
        from service.project_data import ProjectDataService
        project_data_service : ProjectDataService = Singleton.get_instance(id="project_data_service")
        project_data_item = project_data_service.create_project_data(
            data_name = data_name,
            project_id = Singleton.get_value(id="project_id"),
            user_id = Singleton.get_value(id="user_id"),
            bucket = Singleton.get_value(id="project_bucket"),
            data_type = data_type
        )
        project_data_service.push_data_from_bytes(project_data_item, data, length)
    
    @classmethod
    def delete(cls, data_id: str):
        # 删除项目资产
        from service.project_data import ProjectDataService
        project_data_service : ProjectDataService = Singleton.get_instance(id="project_data_service")
        project_data_service.delete_project_data(data_id)
        
        
