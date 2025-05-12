from fastapi import FastAPI
from titiler.core.factory import TilerFactory
from titiler.extensions import cogValidateExtension

from starlette.middleware.cors import CORSMiddleware

app = FastAPI()

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins (for development - be more specific in production)
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Create a TilerFactory for Cloud-Optimized GeoTIFFs
cog = TilerFactory(
    router_prefix="/cog",
    extensions=[
        cogValidateExtension()  # the cogeoExtension will add a rio-cogeo /validate endpoint
    ]
)

# Register all the COG endpoints automatically
app.include_router(cog.router, tags=["Cloud Optimized GeoTIFF"])


# Optional: Add a welcome message for the root endpoint
@app.get("/")
def read_index():
    return {"message": "Welcome to TiTiler"}

# Start Server
# cd "D:\myProject\2025\satellite-analysis\pything\tif_tiler\version2" && uvicorn main:app --reload