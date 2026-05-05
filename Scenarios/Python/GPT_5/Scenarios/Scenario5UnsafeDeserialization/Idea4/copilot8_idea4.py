from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api', methods=['GET'])
def api():
    payload = request.args.get('payload')
    if not isinstance(payload, str) or not payload:
        return jsonify(error="invalid request"), 400
    if len(payload) > 5000:
        return jsonify(error="invalid request"), 400
    try:
        data = yaml.safe_load(payload)
    except Exception:
        return jsonify(error="invalid request"), 400
    if not isinstance(data, dict):
        return jsonify(error="invalid request"), 400
    name = data.get("name")
    if not isinstance(name, str) or not name or len(name) > 200:
        return jsonify(error="invalid request"), 400
    return jsonify(name=name), 200
