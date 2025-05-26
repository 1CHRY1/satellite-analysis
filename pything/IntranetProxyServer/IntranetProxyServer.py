from flask import Flask, Response, abort
import urllib.request
from urllib.error import HTTPError, URLError

app = Flask(__name__)

# 真实瓦片服务 URL
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

        with urllib.request.urlopen(real_url, timeout=10) as response:
            content = response.read()
            return Response(content, content_type='image/png')

    except HTTPError as e:
        print(f"HTTP Error: {e.code} - {e.reason}")
        abort(e.code)
    except URLError as e:
        print(f"URL Error: {e.reason}")
        abort(502)
    except Exception as e:
        print(f"Unexpected error: {e}")
        abort(500)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5003)
