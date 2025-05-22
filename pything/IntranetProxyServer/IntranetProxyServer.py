from flask import Flask, Response, abort
import requests

app = Flask(__name__)

# 你的真实瓦片服务 URL
VEC_TILE_SERVER_URL = "http://172.16.4.53/vec_w_2000"
TXT_TILE_SERVER_URL = "http://172.16.4.53/cva_w_2000"

@app.route('/vec_w_2000/<int:z>/<int:x>/<int:y>.png')
def tile_vec_proxy(z, x, y):
    return proxy_tile(z, x, y, VEC_TILE_SERVER_URL)

@app.route('/cva_w_2000/<int:z>/<int:x>/<int:y>.png')
def tile_txt_proxy(z, x, y):
    return proxy_tile(z, x, y, TXT_TILE_SERVER_URL)

def proxy_tile(z, x, y, base_url):
    try:
        custom_y = y - 2 ** (z - 2)
        real_url = f"{base_url}/L{z}/R{custom_y}/C{x}.png"
        print('Request::', real_url)
        response = requests.get(real_url, timeout=10)
        if response.status_code == 200:
            return Response(response.content, content_type='image/png')
        else:
            abort(response.status_code)
    except Exception as e:
        print(f"Error fetching tile: {e}")
        abort(500)

if __name__ == '__main__':
    app.run(port=5003)