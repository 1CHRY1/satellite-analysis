import math
import time
from typing import Dict, List, Tuple, Any
from dataclasses import dataclass
from shapely.geometry import Polygon, box

# Constants
EARTH_CIRCUMFERENCE_EQUATOR = 40075.0
EARTH_CIRCUMFERENCE_MERIDIAN = 40008.0


@dataclass
class Geometry:
    type: str
    coordinates: List[List[List[float]]]

@dataclass
class Feature:
    type: str
    geometry: Geometry
    properties: Dict[str, Any]

@dataclass
class GeoPolygon:
    type: str  # "FeatureCollection"
    features: List[Feature]

@dataclass
class GridCell:
    columnId: int
    rowId: int



class GridHelper:
    def __init__(self, grid_resolution_in_kilometer: float = 1):
        """
        Initialize the GridHelper with grid resolution.
        
        Args:
            grid_resolution_in_kilometer: Grid resolution in kilometers
        """
        
        print(grid_resolution_in_kilometer)
        
        self.degreePerGridX = (360.0 * grid_resolution_in_kilometer) / EARTH_CIRCUMFERENCE_EQUATOR
        self.degreePerGridY = (180.0 * grid_resolution_in_kilometer) / EARTH_CIRCUMFERENCE_MERIDIAN * 2.0

        self.grid_num_x = int(math.ceil(360.0 / self.degreePerGridX))
        self.grid_num_y = int(math.ceil(180.0 / self.degreePerGridY))

    def get_grid_cells(self, polygon: GeoPolygon) -> List[GridCell]:
        """
        Get grid cells that intersect with the input polygon.
        
        Args:
            polygon: Input polygon geometry
            
        Returns:
            List of GridCell objects that intersect with the polygon
        """
        start_time = time.time()
        
        # Convert input polygon to Shapely polygon
        input_poly = self._create_shapely_polygon(polygon)
        
        # First get approximate grid range using bbox
        bbox = self._calculate_bbox(polygon)
        top_left, bottom_right = bbox['topLeft'], bbox['bottomRight']
        min_lng, max_lat = top_left
        max_lng, min_lat = bottom_right
        
        # Calculate grid index range (expanded by 1 to ensure we cover edge cases)
        start_grid_x = max(0, math.floor(((min_lng + 180) / 360) * self.grid_num_x) - 1)
        end_grid_x = min(self.grid_num_x, math.ceil(((max_lng + 180) / 360) * self.grid_num_x) + 1)
        start_grid_y = max(0, math.floor(((90 - max_lat) / 180) * self.grid_num_y) - 1)
        end_grid_y = min(self.grid_num_y, math.ceil(((90 - min_lat) / 180) * self.grid_num_y) + 1)
        
        grid_cells = []
        
        # Check each grid cell in the approximate range
        for i in range(start_grid_x, end_grid_x):
            for j in range(start_grid_y, end_grid_y):
                # Get grid cell polygon
                grid_poly_box = self._get_grid_polygon(i, j)
                
                # Check intersection
                if input_poly.intersects(grid_poly_box):
                    grid_cells.append(GridCell(columnId=i, rowId=j))

        print(f"Found {len(grid_cells)} intersecting grid cells in {(time.time() - start_time) * 1000:.2f}ms")
        return grid_cells

    def get_grid_cell(self, longitude: float, latitude: float) -> GridCell:
        """
        Get the grid cell that contains the input longitude and latitude.
        
        Args:
            longitude: Longitude coordinate 
            latitude: Latitude coordinate
            
        Returns:
            GridCell object that contains the input longitude and latitude
        """
        grid_x = math.floor(((longitude + 180) / 360) * self.grid_num_x)
        grid_y = math.floor(((90 - latitude) / 180) * self.grid_num_y)
        return GridCell(columnId=grid_x, rowId=grid_y)

    def get_grid_cells_by_bbox(self, bbox: List[float]) -> List[GridCell]:

        left_bottom_grid = self.get_grid_cell(bbox[0], bbox[1])

        right_top_grid = self.get_grid_cell(bbox[2], bbox[3])

        grid_cells = []

        for i in range(left_bottom_grid.columnId, right_top_grid.columnId + 1):
            for j in range(right_top_grid.rowId, left_bottom_grid.rowId + 1):
                grid_cells.append(GridCell(columnId=i, rowId=j))

        return grid_cells
    
    def get_grid_bbox(self, grid_cell: GridCell) -> List[float]:
        """Get the bounding box of a grid cell."""
        left_bottom_lng, left_bottom_lat = self._grid_to_lnglat(grid_cell.columnId, grid_cell.rowId + 1)
        right_top_lng, right_top_lat = self._grid_to_lnglat(grid_cell.columnId + 1, grid_cell.rowId)
        return [left_bottom_lng, left_bottom_lat, right_top_lng, right_top_lat]
    
    def grid_to_geojsonfeature(self, grid_cell: GridCell) -> Dict:
        """Convert a grid cell to a GeoJSON polygon feature."""
        left_bottom_lng, left_bottom_lat = self._grid_to_lnglat(grid_cell.columnId, grid_cell.rowId + 1)
        right_top_lng, right_top_lat = self._grid_to_lnglat(grid_cell.columnId + 1, grid_cell.rowId)

        return {
            "type": "Feature",
            "geometry": {
                "type": "Polygon",
                "coordinates": [
                    [
                        [left_bottom_lng, left_bottom_lat],
                        [right_top_lng, left_bottom_lat],
                        [right_top_lng, right_top_lat],
                        [left_bottom_lng, right_top_lat],
                        [left_bottom_lng, left_bottom_lat]
                    ]
                ]
            }
        }
        
    def grid_to_geojson(self, grid_cells: List[GridCell]) -> Dict:
        """Convert a list of grid cells to a GeoJSON FeatureCollection."""
        features = [self.grid_to_geojsonfeature(grid_cell) for grid_cell in grid_cells]
        return {
            "type": "FeatureCollection",
            "features": features
        }
        
        
    def _create_shapely_polygon(self, polygon: GeoPolygon) -> Polygon:
        """Convert GeoJSON-like polygon to Shapely polygon."""
        return Polygon(polygon.get("features")[0].get("geometry").get("coordinates")[0])

    def _get_grid_polygon(self, grid_x: int, grid_y: int) -> Polygon:
        """Create Shapely polygon for a grid cell."""
        left_lng, top_lat = self._grid_to_lnglat(grid_x, grid_y)
        right_lng, bottom_lat = self._grid_to_lnglat(grid_x + 1, grid_y + 1)
        return box(left_lng, bottom_lat, right_lng, top_lat)

    def _grid_to_lnglat(self, grid_x: int, grid_y: int) -> Tuple[float, float]:
        """Convert grid coordinates to longitude/latitude."""
        lng = (grid_x / self.grid_num_x) * 360.0 - 180.0
        lat = 90.0 - (grid_y / self.grid_num_y) * 180.0
        return lng, lat

    def _calculate_bbox(self, polygon: GeoPolygon) -> Dict[str, List[float]]:
        """Calculate bounding box of a polygon."""
        coordinates = polygon.get("features")[0].get("geometry").get("coordinates")[0]
        min_lng = min(coord[0] for coord in coordinates)
        max_lng = max(coord[0] for coord in coordinates)
        min_lat = min(coord[1] for coord in coordinates)
        max_lat = max(coord[1] for coord in coordinates)
        
        return {
            "topLeft": [min_lng, max_lat],
            "bottomRight": [max_lng, min_lat]
        }
        