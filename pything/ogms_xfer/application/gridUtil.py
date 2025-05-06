import math
import time
from typing import Dict, List, Tuple
from dataclasses import dataclass
from shapely.geometry import Polygon, box

# Constants
EARTH_RADIUS = 6371008.8  # meters
EARTH_CIRCUMFERENCE = 2 * math.pi * EARTH_RADIUS

@dataclass
class PolygonGeometry:
    type: str
    coordinates: List[List[List[float]]]

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
        self.grid_resolution_in_meter = grid_resolution_in_kilometer * 1000
        self.grid_num_x = math.ceil(EARTH_CIRCUMFERENCE / self.grid_resolution_in_meter)
        self.grid_num_y = math.ceil(EARTH_CIRCUMFERENCE / 2.0 / self.grid_resolution_in_meter)

    def get_grid_cells(self, polygon: PolygonGeometry) -> List[GridCell]:
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


    def _create_shapely_polygon(self, polygon: PolygonGeometry) -> Polygon:
        """Convert GeoJSON-like polygon to Shapely polygon."""
        print(polygon.get("features")[0].get("geometry"))
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

    def _calculate_bbox(self, polygon: PolygonGeometry) -> Dict[str, List[float]]:
        """Calculate bounding box of a polygon."""
        coordinates = polygon.get("features")[0].get("geometry").get("coordinates")[0]
        min_x = min(coord[0] for coord in coordinates)
        max_x = max(coord[0] for coord in coordinates)
        min_y = min(coord[1] for coord in coordinates)
        max_y = max(coord[1] for coord in coordinates)
        
        return {
            "topLeft": [min_x, max_y],
            "bottomRight": [max_x, min_y]
        }