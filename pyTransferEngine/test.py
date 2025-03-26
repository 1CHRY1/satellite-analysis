from TransferEngine.engine import TransferEngine

if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    
    TransferEngine.initialize(config_file_path)

    # Testing
    sensor = TransferEngine.Sensor("S1234567890")
    if sensor is not None:
        print(sensor)
    else:
        print("传感器不存在")

    # sensor = TransferEngine.Sensor("SE15303")
    # products = sensor.get_all_products()
    # scenes = products[0].get_all_scenes()
    # images = scenes[0].get_all_band_images()
    # print('testing object ending')
    
    # target_scene = TransferEngine.Scene("SC153032082")
    # image = target_scene.get_all_band_images()[0]
    # image.pull("D:\\t\\2\\hahaha.tif")
    
    # tiles1 = target_scene.get_tiles_by_cloud(100)
    # print(len(tiles1))

    # tile_ids = []
    # for i in range(10):
    #     tile_ids.append(tiles1[i].tile_id)
    #     image.pull_tiles_by_ids(tile_ids, "D:\\t\\2")
    
    # 测试项目数据传输
    # TransferEngine.ProjectDataTransfer().upload("test", "test", "D:\\t\\2\\hahaha.tif")
    transfer = TransferEngine.ProjectDataTransfer()
    # transfer.upload("jklove.json", "json", "D:\\t\\2\\testing.json")
    # transfer.download("D6756188294", "D:\\t\\2\\jjk.json")
    # transfer.delete("D6756188294")
    print(' --------------------------------- ')
    print('great!')