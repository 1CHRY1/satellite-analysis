from dataProcessing.model.methlib.parsers.base import ParameterHandler


class PrimitiveTypeHandler(ParameterHandler):
    """处理基本类型参数: Boolean, Integer, Float, String, StringOrNumber"""

    SUPPORTED_TYPES = {"Boolean", "Integer", "Float", "String", "StringOrNumber"}

    def supports(self, parameter_type):
        if isinstance(parameter_type, str):
            return parameter_type in self.SUPPORTED_TYPES
        return False

    def parse(self, parameter_type, raw_value, val_index, context, is_external):
        if raw_value is None:
            self.handle_null(raw_value, context.cmd_builder, context.param_specs)
        else:
            context.cmd_builder.append(f"{str(raw_value)} ")
