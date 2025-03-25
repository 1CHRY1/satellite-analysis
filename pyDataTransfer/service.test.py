from connection.database import DatabaseClient
from service.project_data import ProjectDataService
from service.sensor import SensorService
from service.image import ImageService
from service.product import ProductService
from service.scene import SceneService
from service.tile import TileService
from config import MYSQL_HOST, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE,MYSQL_TILE_DATABASE, MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_SECURE
from connection.minio import MinioClient

def test_services():
    
    ##### sattellite数据库测试 ###################################
    db_engine = DatabaseClient(
        host=MYSQL_HOST,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=MYSQL_DATABASE
    )

    minio_client = MinioClient(
        endpoint=MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=MINIO_SECURE
    )

    with db_engine.get_db() as db:
        
        if(True) : 
            # 测试 SensorService
            sensor_service = SensorService(db)
            sensor = sensor_service.get_by_id("SE33955")
            # print("Sensor:", sensor)
            
            print("\n----------------------------------------------\n")

            all_sensors = sensor_service.get_all()
            # print("All Sensors:", all_sensors)

            print("\n----------------------------------------------\n")
            
            filtered_sensors = sensor_service.filter_by_name("land")
            # print("Filtered Sensors:", filtered_sensors)
        
        
        if(True) : 
            # 测试 ImageService
            image_service = ImageService(db, minio_client)
            image = image_service.get_by_id("I1611320860")  # 替换为实际的 image_id
            print("Image:", image)
            # image_service.pull_image(image, "D:\\t\\1\\I1611320860.tif")

            print("\n----------------------------------------------\n")

            all_images = image_service.get_all()
            # print("All Images:", all_images)

            print("\n----------------------------------------------\n")

            filtered_images = image_service.filter_by_scene_id("SC339559283")  # 替换为实际的 image_name
            # print("Filtered Images:", filtered_images)

        
        if(True) : 
            # 测试 ProductService
            product_service = ProductService(db)
            product = product_service.get_by_id("P33955928")
            # print("Product:", product)

            print("\n----------------------------------------------\n")

            all_products = product_service.get_all()
            # print("All Products:", all_products)

            print("\n----------------------------------------------\n")

            filtered_products = product_service.filter_by_name("8") 
            # print("Filtered Products:", filtered_products)

        
        if(True) : 
            # 测试 SceneService
            scene_service = SceneService(db)
            scene = scene_service.get_by_id("SC339559282")  # 替换为实际的 scene_id
            # print("Scene:", scene)

            all_scenes = scene_service.get_all()
            # print("All Scenes:", all_scenes)

            filtered_scenes = scene_service.filter_by_name("9")  # 替换为实际的 scene_name
            # print("Filtered Scenes:", filtered_scenes)
            
            
        if(True) : 
            # 测试 ProjectDataService
            project_data_service = ProjectDataService(db, minio_client)
            
                    
            # # 删除项目数据
            # project_data_array =  project_data_service.filter_by_name("test")
            # for pd in project_data_array:
            #     project_data_service.delete_project_data(pd)
            
            # 创建项目数据
            project_data = project_data_service.create_project_data(
                data_name="test5.tif",
                project_id="P33955928",
                user_id="U33955928",
                bucket="project-data-bucket",
                data_type="tif"
            )
            project_data_service.push_data(project_data, "D:\\t\\1\\hahahhh.tif")
            
            # 下载项目数据
            # project_data =  project_data_service.filter_by_name("test")[0]
            # project_data_service.pull_data(project_data, "D:\\t\\1\\pullhh.tif")
        
        
            
            print("\n----------------------------------------------\n")
            
            all_project_data = project_data_service.get_all()
            # print("All ProjectData:", all_project_data)
            
            
            
        
    ##### tile数据库测试 #########################################
    db_engine = DatabaseClient(
        host=MYSQL_HOST,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
        database=MYSQL_TILE_DATABASE
    )

    # 因为瓦片表是动态表，此处数据库session已在内部自动创建，自动关闭
    tile_service = TileService(db_engine, minio_client)
    
    # tiles = tile_service.get_tiles_by_scene("I3395592835") # successed but too slow
    print("\n--------------------------------------------------------------\n")
    
    # get tile by id
    tile = tile_service.get_tile_by_id("I3395592835", "00026b57-71cd-41f3-abb0-1651d789cad7")
    # print("tile:",tile)
    print("\n--------------------------------------------------------------\n")
    
    # # pull tile by id
    # output_path = "D:\\t\\1\\hahahhh.tif"
    # tile_service.pull_tile_by_id("I3395592835", "000cb10c-4d1c-4e42-af74-d48ee62c6d53", output_path)
    # print("\n--------------------------------------------------------------\n")
    
    # # pull tiles by ids
    # output_dir = "D:\\t\\1"
    # tile_ids = ["000cb10c-4d1c-4e42-af74-d48ee62c6d53", "000dfc1f-bafe-4867-a6e7-e3d0e40cf3bb", "000ebf7d-7b06-4943-825c-0182dbd8a6ee"]
    # tile_service.pull_tiles_by_ids("I3395592835", tile_ids, output_dir)



if __name__ == "__main__":
    test_services()