from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.declarative import declarative_base
from contextlib import contextmanager
from geoalchemy2 import Geometry  # <= not used but must be imported

class DatabaseClient:
    
    Base = declarative_base()
    
    def __init__(self, host:str, user:str, password:str, database:str):
        self.DATABASE_URL = f"mysql+pymysql://{user}:{password}@{host}/{database}"
        self.engine = create_engine(self.DATABASE_URL)
        self.SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)

    @contextmanager
    def get_db(self):
        db = self.SessionLocal()
        try:
            yield db
        finally:
            db.close()
            print("db closed")