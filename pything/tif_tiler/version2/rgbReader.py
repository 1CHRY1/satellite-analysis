from typing import Dict
from rio_tiler.io import MultiBandReader
from fastapi import Query

class RGBReader(MultiBandReader):
    def __init__(self, urls: Dict[str, str]):
        self.band_urls = urls
        super().__init__(bands=list(urls.keys()))

    def _get_band_url(self, band: str) -> str:
        return self.band_urls[band]
    
def RGBPathParams(
    r: str = Query(..., description="Red band COG URL"),
    g: str = Query(..., description="Green band COG URL"),
    b: str = Query(..., description="Blue band COG URL")
) -> RGBReader:
    urls = {
        "red": r,
        "green": g,
        "blue": b,
    }
    return RGBReader(urls)