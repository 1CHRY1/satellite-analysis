"""Default Morecantile TMS."""

import os
import pathlib
from copy import copy
from typing import Dict, List, Union

import attr

from morecantile.errors import InvalidIdentifier
from morecantile.models import TileMatrixSet

morecantile_tms_dir = pathlib.Path(__file__).parent.joinpath("data")
tms_paths = list(pathlib.Path(morecantile_tms_dir).glob("*.json"))

user_tms_dir = os.environ.get("TILEMATRIXSET_DIRECTORY", None)
if user_tms_dir:
    tms_paths.extend(list(pathlib.Path(user_tms_dir).glob("*.json")))

default_tms: Dict[str, Union[TileMatrixSet, pathlib.Path]] = {
    tms.stem: tms for tms in sorted(tms_paths)
}


@attr.s(frozen=True)
class TileMatrixSets:
    """Default TileMatrixSets holder."""

    tms: Dict = attr.ib()
    web_mercator_quad = {
   "id": "WebMercatorQuad",
   "title": "Google Maps Compatible for the World",
   "uri": "http://www.opengis.net/def/tilematrixset/OGC/1.0/WebMercatorQuad",
   "crs": "http://www.opengis.net/def/crs/EPSG/0/3857",
   "orderedAxes": ["X", "Y"],
   "wellKnownScaleSet": "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible",
   "tileMatrices":
   [
      {
        "id": "0",
        "scaleDenominator": 559082264.028717,
        "cellSize": 156543.033928041,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 1,
        "matrixHeight": 1
      },
      {
        "id": "1",
        "scaleDenominator": 279541132.014358,
        "cellSize": 78271.5169640204,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 2,
        "matrixHeight": 2
      },
      {
        "id": "2",
        "scaleDenominator": 139770566.007179,
        "cellSize": 39135.7584820102,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 4,
        "matrixHeight": 4
      },
      {
        "id": "3",
        "scaleDenominator": 69885283.0035897,
        "cellSize": 19567.8792410051,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 8,
        "matrixHeight": 8
      },
      {
        "id": "4",
        "scaleDenominator": 34942641.5017948,
        "cellSize": 9783.93962050256,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 16,
        "matrixHeight": 16
      },
      {
        "id": "5",
        "scaleDenominator": 17471320.7508974,
        "cellSize": 4891.96981025128,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 32,
        "matrixHeight": 32
      },
      {
        "id": "6",
        "scaleDenominator": 8735660.37544871,
        "cellSize": 2445.98490512564,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 64,
        "matrixHeight": 64
      },
      {
        "id": "7",
        "scaleDenominator": 4367830.18772435,
        "cellSize": 1222.99245256282,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 128,
        "matrixHeight": 128
      },
      {
        "id": "8",
        "scaleDenominator": 2183915.09386217,
        "cellSize": 611.49622628141,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 256,
        "matrixHeight": 256
      },
      {
        "id": "9",
        "scaleDenominator": 1091957.54693108,
        "cellSize": 305.748113140704,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 512,
        "matrixHeight": 512
      },
      {
        "id": "10",
        "scaleDenominator": 545978.773465544,
        "cellSize": 152.874056570352,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 1024,
        "matrixHeight": 1024
      },
      {
        "id": "11",
        "scaleDenominator": 272989.386732772,
        "cellSize": 76.4370282851762,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 2048,
        "matrixHeight": 2048
      },
      {
        "id": "12",
        "scaleDenominator": 136494.693366386,
        "cellSize": 38.2185141425881,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 4096,
        "matrixHeight": 4096
      },
      {
        "id": "13",
        "scaleDenominator": 68247.346683193,
        "cellSize": 19.109257071294,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 8192,
        "matrixHeight": 8192
      },
      {
        "id": "14",
        "scaleDenominator": 34123.6733415964,
        "cellSize": 9.55462853564703,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 16384,
        "matrixHeight": 16384
      },
      {
        "id": "15",
        "scaleDenominator": 17061.8366707982,
        "cellSize": 4.77731426782351,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 32768,
        "matrixHeight": 32768
      },
      {
        "id": "16",
        "scaleDenominator": 8530.91833539913,
        "cellSize": 2.38865713391175,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 65536,
        "matrixHeight": 65536
      },
      {
        "id": "17",
        "scaleDenominator": 4265.45916769956,
        "cellSize": 1.19432856695587,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 131072,
        "matrixHeight": 131072
      },
      {
        "id": "18",
        "scaleDenominator": 2132.72958384978,
        "cellSize": 0.597164283477939,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 262144,
        "matrixHeight": 262144
      },
      {
        "id": "19",
        "scaleDenominator": 1066.36479192489,
        "cellSize": 0.29858214173897,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 524288,
        "matrixHeight": 524288
      },
      {
        "id": "20",
        "scaleDenominator": 533.182395962445,
        "cellSize": 0.149291070869485,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 1048576,
        "matrixHeight": 1048576
      },
      {
        "id": "21",
        "scaleDenominator": 266.591197981222,
        "cellSize": 0.0746455354347424,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 2097152,
        "matrixHeight": 2097152
      },
      {
        "id": "22",
        "scaleDenominator": 133.295598990611,
        "cellSize": 0.0373227677173712,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 4194304,
        "matrixHeight": 4194304
      },
      {
        "id": "23",
        "scaleDenominator": 66.6477994953056,
        "cellSize": 0.0186613838586856,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 8388608,
        "matrixHeight": 8388608
      },
      {
        "id": "24",
        "scaleDenominator": 33.3238997476528,
        "cellSize": 0.0093306919293428,
        "pointOfOrigin": [-20037508.342789244,20037508.342789244],
        "tileWidth": 256,
        "tileHeight": 256,
        "matrixWidth": 16777216,
        "matrixHeight": 16777216
      }
   ]
}


    def get(self, identifier: str) -> TileMatrixSet:
        self.tms["WebMercatorQuad"] = self.web_mercator_quad
        """Fetch a TMS."""
        if identifier not in self.tms:
            raise InvalidIdentifier(f"Invalid identifier: {identifier}")

        tms = self.tms[identifier]

        # We lazyload the TMS document only when called
        if isinstance(tms, pathlib.Path):
            with tms.open() as f:
                tms = TileMatrixSet.model_validate_json(f.read())
                self.tms[identifier] = tms

        return tms

    def list(self) -> List[str]:
        """List registered TMS."""
        return list(self.tms.keys())

    def register(
        self,
        custom_tms: Dict[str, TileMatrixSet],
        overwrite: bool = False,
    ) -> "TileMatrixSets":
        """Register TileMatrixSet(s)."""
        for identifier in custom_tms.keys():
            if identifier in self.tms and not overwrite:
                raise InvalidIdentifier(f"{identifier} is already a registered TMS.")

        return TileMatrixSets({**self.tms, **custom_tms})


tms = TileMatrixSets(copy(default_tms))  # noqa
