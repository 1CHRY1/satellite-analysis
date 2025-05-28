from flask import Flask, Response, abort
from flask_cors import CORS
import asyncpg
import asyncio

app = Flask(__name__)
CORS(app)

DB_CONFIG = {
    "user": "postgres",
    "password": "123456",       # 替换为实际密码
    "database": "gis_db",       # 替换为实际数据库名
    "host": "223.2.43.228",
    "port": 31543
}

TABLE_NAME = "patch_table"
GEOM_COLUMN = "geom"
SRID_MVT = 3857
MVT_LAYER_NAME = "patches"

async def fetch_tile(z, x, y):
    sql = f"""
        WITH bounds AS (
            SELECT ST_TileEnvelope({z}, {x}, {y}) AS geom
        ),
        mvtgeom AS (
            SELECT ST_AsMVTGeom(
                ST_Transform(t.{GEOM_COLUMN}, {SRID_MVT}),
                b.geom
            ) AS geom
            FROM {TABLE_NAME} t, bounds b
            WHERE ST_Intersects(
                ST_Transform(t.{GEOM_COLUMN}, {SRID_MVT}),
                b.geom
            )
        )
        SELECT ST_AsMVT(mvtgeom, '{MVT_LAYER_NAME}', 4096, 'geom') FROM mvtgeom;
    """
    try:
        conn = await asyncpg.connect(**DB_CONFIG)
        row = await conn.fetchrow(sql)
        await conn.close()
        return row[0] if row else None
    except Exception as e:
        raise RuntimeError(str(e))

@app.route("/tiles/<int:z>/<int:x>/<int:y>")
def get_tile(z, x, y):
    try:
        tile = asyncio.run(fetch_tile(z, x, y))
        if tile:
            return Response(response=bytes(tile), content_type="application/x-protobuf")
        else:
            abort(404, description="Tile not found")
    except Exception as e:
        abort(500, description=str(e))

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
