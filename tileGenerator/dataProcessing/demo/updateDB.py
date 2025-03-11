import sys
import os

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(project_root)

from Utils.mySqlUtils import delete_DB, create_DB

delete_DB()
create_DB()