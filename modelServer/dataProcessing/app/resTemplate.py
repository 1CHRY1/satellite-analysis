import datetime
from flask import jsonify


def api_response(code=200, message="success", data=None):
    """
    生成标准 API 响应格式
    :param code: 状态码（默认200）
    :param message: 返回消息（默认"success"）
    :param data: 返回数据（可选）
    :return: JSON 格式的响应
    """
    response = {
        "code": code,
        "message": message,
        "data": data,
        "timestamp": datetime.datetime.utcnow().isoformat()
    }
    return jsonify(response), code
