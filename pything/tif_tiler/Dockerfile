FROM python:3.12-slim

# 添加软件源配置（如 sources.list 不存在则创建）
RUN echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian/ buster main non-free contrib" > /etc/apt/sources.list && \
    echo "deb-src https://mirrors.tuna.tsinghua.edu.cn/debian/ buster main non-free contrib" >> /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    && apt-get update && apt-get install -y \
        build-essential \
        gdal-bin \
        libgdal-dev \
        curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 设置 pip 国内镜像（清华源）
# cogeo-mosaic携带了shapely库
RUN pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
    fastapi \
    "uvicorn[standard]" \
    titiler.core==0.22.2 \
    titiler.extensions==0.22.2 \
    rio-cogeo==5.4.1 \
    cogeo-mosaic==8.1.0 \
    numpy==1.26.4 \
    rio-color==2.0.1 \
    rio-mucho==1.0.0 \
    rio-rgbify==0.4.0 \
    rio-stac==0.8.1 \
    rio-tiler==7.7.4 \
    rio-tiler-mosaic==0.0.1.dev5 \
    titiler==0.22.2 \
    titiler-application==0.22.2 \
    titiler-core==0.22.2 \
    titiler-extensions==0.22.2 \
    titiler-mosaic==0.22.2 \
    titiler-xarray==0.22.2 \
    mercantile==1.2.1 \
    minio==7.2.15 \
    setuptools==75.1.0
    