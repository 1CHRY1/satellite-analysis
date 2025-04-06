from .models.landslide import landslide_probability_model

class ModelStore:
    """模型管理器，用于统一管理所有模型"""
    
    def __init__(self):
        # 初始化所有模型
        self.landslide_probability_model = landslide_probability_model
        
    def get_model(self, model_name: str):
        """获取指定的模型"""
        if hasattr(self, model_name):
            return getattr(self, model_name)
        raise AttributeError(f"Model {model_name} not found in ModelStore")
        
    def list_models(self):
        """列出所有可用的模型"""
        return [attr for attr in dir(self) if not attr.startswith('_') and not callable(getattr(self, attr))] 