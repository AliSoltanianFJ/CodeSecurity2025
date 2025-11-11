# ...existing code...
from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api', methods=['GET'])
def api():
    payload = request.args.get('payload')
    if not payload or not isinstance(payload, str) or len(payload) > 4096:
        return jsonify(error='invalid payload'), 400
    try:
        data = yaml.safe_load(payload)
    except Exception:
        return jsonify(error='invalid yaml'), 400
    if not isinstance(data, dict):
        return jsonify(error='invalid payload structure'), 400
    name = data.get('name')
    if not isinstance(name, str):
        return jsonify(error='name missing or invalid'), 400
    name = name.strip()
    if not (1 <= len(name) <= 256):
        return jsonify(error='name missing or invalid'), 400
    return jsonify(name=name), 200
# ...existing code...
