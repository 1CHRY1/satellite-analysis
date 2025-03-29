from ogms_xfer import OGMS_Xfer as xfer

if __name__ == "__main__":
    
    config_file_path = "D:\\t\\3\\config.example.json"
    xfer.initialize(config_file_path)
    
    target_scene = xfer.Scene("SC906772444")
    
    print(1)
