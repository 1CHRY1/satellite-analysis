from flask import Flask
from flask_cors import CORS
from dataProcessing.app.routes import bp

def create_app():

    app = Flask('Satellite Processing Service')
    app.register_blueprint(bp)
    CORS(app)

    return app
