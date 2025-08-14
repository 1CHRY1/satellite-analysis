FROM python:3.9-slim
    
# 替换 apt 镜像源为国内镜像源（例如阿里云或清华大学）
RUN echo "deb https://mirrors.tuna.tsinghua.edu.cn/debian/ buster main non-free contrib" > /etc/apt/sources.list && \
    echo "deb-src https://mirrors.tuna.tsinghua.edu.cn/debian/ buster main non-free contrib" >> /etc/apt/sources.list && \
    apt-get update && apt-get install -y --no-install-recommends \
    g++ \
    gcc \
    build-essential \
    python3-dev \
    libgdal-dev \
    gdal-bin \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 设置环境变量（确保 pip 安装 GDAL 时能找到对应的 gdal-config）
ENV CPLUS_INCLUDE_PATH=/usr/include/gdal
ENV C_INCLUDE_PATH=/usr/include/gdal
ENV GDAL_VERSION=3.2.2


# 先单独安装GDAL和rasterio（使用兼容版本）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
    GDAL==${GDAL_VERSION} \
    rasterio==1.4.3


# 安装 Python 依赖（注意 GDAL 的版本需与系统匹配）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple \
	affine==2.4.0 \
	annotated-types==0.7.0 \
	argon2-cffi==23.1.0 \
	argon2-cffi-bindings==21.2.0 \
	attrs==25.3.0 \
	cachetools==5.5.2 \
	certifi==2025.1.31 \
	cffi==1.17.1 \
	click==8.1.8 \
	click-plugins==1.1.1 \
	cligj==0.7.2 \
	GeoAlchemy2==0.17.1 \
	greenlet==3.1.1 \
	importlib_metadata==8.6.1 \
	minio==7.2.15 \
	morecantile==6.2.0 \
	numpy==1.26.4 \
	packaging==24.2 \
	pandas==2.2.3 \
	pycparser==2.22 \
	pycryptodome==3.22.0 \
	pydantic==2.11.0 \
	pydantic_core==2.33.0 \
	PyMySQL==1.1.1 \
	pyparsing==3.2.3 \
	pyproj==3.6.1 \
	python-dateutil==2.9.0.post0 \
	pytz==2025.2 \
	rio-cogeo==5.4.1 \
	setuptools==57.5.0 \
	shapely==2.0.7 \
	six==1.17.0 \
	SQLAlchemy==2.0.39 \
	typing_extensions==4.13.0 \
	typing-inspection==0.4.0 \
	tzdata==2025.2 \
	urllib3==2.3.0 \
	watchdog==6.0.0 \
	wheel==0.40.0 \
	zipp==3.21.0 \
	rio-tiler==7.8.0 \
  ray==2.46.0

