from .provider import Singleton

class Vector:
    def __init__(self, table_or_sql: str = None):
        """
        table_or_sql: could be a table name or a raw SQL query logic. 
        For now, let's treat it as a source identifier.
        """
        self.source = table_or_sql
        self.db_client = Singleton.get_instance("vector-database")

    def read(self, sql: str = None, geom_col: str = 'geom'):
        """
        Read vector data from PostGIS.
        If geopandas is available, returns GeoDataFrame.
        Otherwise, returns pandas DataFrame (geometry as raw/hex).
        """
        query = sql
        if query is None and self.source:
            query = f"SELECT * FROM {self.source}"
        
        if query:
            try:
                import geopandas as gpd
                return gpd.read_postgis(query, self.db_client.engine, geom_col=geom_col)
            except ImportError:
                import pandas as pd
                # print("Warning: 'geopandas' not found, falling back to pandas.read_sql (geometry will be raw).")
                return pd.read_sql(query, self.db_client.engine)
        return None

    def execute(self, sql: str):
        """
        Execute raw SQL (non-query or custom).
        """
        from sqlalchemy import text
        with self.db_client.engine.connect() as conn:
            return conn.execute(text(sql))
