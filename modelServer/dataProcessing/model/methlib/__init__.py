from typing import List
# 假设这些类在 methlib 包内的相应模块中
from dataProcessing.model.methlib.parsers.base import ParameterHandler
from dataProcessing.model.methlib.parsers.handler.directory import DirectoryHandler
from dataProcessing.model.methlib.parsers.handler.existing_file import ExistingFileHandler
from dataProcessing.model.methlib.parsers.handler.existing_file_or_float import ExistingFileOrFloatHandler
from dataProcessing.model.methlib.parsers.handler.file_list import FileListHandler
from dataProcessing.model.methlib.parsers.handler.new_file import NewFileHandler
from dataProcessing.model.methlib.parsers.handler.option_list import OptionListHandler
from dataProcessing.model.methlib.parsers.handler.primitive_type import PrimitiveTypeHandler
from dataProcessing.model.methlib.parsers.factory import ParameterHandlerFactory
from dataProcessing.model.methlib.parsers.handler.vector_attribute_field import VectorAttributeFieldHandler
from dataProcessing.config import current_config as CONFIG


# 定义全局变量，外部模块将通过导入它来访问工厂
GLOBAL_HANDLER_FACTORY: ParameterHandlerFactory = None

def init_handler_factory() -> ParameterHandlerFactory:
    """
    初始化所有参数处理器，并将它们组装到全局工厂中。
    该函数设计为幂等（只执行一次初始化）。
    """
    global GLOBAL_HANDLER_FACTORY
    
    if GLOBAL_HANDLER_FACTORY is None:
        # 实例化所有具体的处理器
        existingFileHandler = ExistingFileHandler()
        primitiveTypeHandler = PrimitiveTypeHandler()
        all_handlers: List[ParameterHandler] = [
            PrimitiveTypeHandler(),
            ExistingFileHandler(),
            ExistingFileOrFloatHandler(existingFileHandler, primitiveTypeHandler),
            DirectoryHandler(),
            FileListHandler(),
            NewFileHandler(),
            OptionListHandler(),
            VectorAttributeFieldHandler()
            # TODO: 在这里添加所有其他的处理器实例
        ]
        
        # 实例化工厂，并赋值给全局变量
        GLOBAL_HANDLER_FACTORY = ParameterHandlerFactory(handlers=all_handlers)
        print("ParameterHandlerFactory 初始化完成并设置为全局可用 (在 methlib 包中)。")
    
    return GLOBAL_HANDLER_FACTORY