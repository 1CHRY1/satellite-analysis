FROM python:3.12-slim

# 替换 apt 镜像源为国内镜像源（例如阿里云或清华大学）
RUN echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian/ bookworm main non-free contrib" > /etc/apt/sources.list && \
    echo "deb-src https://mirrors.tuna.tsinghua.edu.cn/debian/ bookworm main non-free contrib" >> /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    g++ \
    gcc \
    build-essential \
    python3-dev \
    libgdal-dev \
    gdal-bin \
    curl \
    libpq-dev \
    # 假设 WhiteboxTools 依赖 GLIBC, Python 3.12-slim 已经基于 Debian Bookworm, 通常不需要额外安装其他运行时库。
    && rm -rf /var/lib/apt/lists/*

# 设置环境变量（确保 pip 安装 GDAL 时能找到对应的 gdal-config）
ENV CPLUS_INCLUDE_PATH=/usr/include/gdal
ENV C_INCLUDE_PATH=/usr/include/gdal
ENV GDAL_VERSION=3.6.2

# 安装 Python 依赖
RUN pip install --no-cache-dir  \
    numpy \
    GDAL==${GDAL_VERSION} \
    flask \
    flask-cors \
    mysql-connector-python \
    pymysql \
    psycopg2-binary \
    minio \
    rasterio \
    rio-cogeo \
    rio-tiler \
    Pillow \
    APScheduler \
    shapely \
    requests \
    ray[default] \
    mercantile