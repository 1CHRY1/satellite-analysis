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
        If sql is provided, execute it.
        Otherwise, query the table defined in self.source.
        """
        import geopandas as gpd
        
        query = sql
        if query is None and self.source:
            # Basic validation to prevent SQL injection if possible, or just raw formatting
            # Assuming source is a table name
            query = f"SELECT * FROM {self.source}"
        
        if query:
            return gpd.read_postgis(query, self.db_client.engine, geom_col=geom_col)
        return None

    def execute(self, sql: str):
        """
        Execute raw SQL (non-query or custom).
        """
        from sqlalchemy import text
        with self.db_client.engine.connect() as conn:
            return conn.execute(text(sql))
