from flask import Flask, request, jsonify
import logging

# Basic logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

@app.route("/", methods=["POST"])
def handle():
    # Example: parse JSON body (ignore validation for now)
    data = request.get_json(silent=True) or {}
    logger.info("Received payload: %s", data)

    # TODO: User fills in their processing logic below.
    # Keep it fast and non-blocking if possible.
    # For now, just echo back.
    result = {
        "status": "ok",
        "echo": data
    }
    return jsonify(result), 200

if __name__ == "__main__":
    # IMPORTANT: Bind to 0.0.0.0 and keep port in sync with serviceDefaults.internalPort
    app.run(host="0.0.0.0", port=9000)