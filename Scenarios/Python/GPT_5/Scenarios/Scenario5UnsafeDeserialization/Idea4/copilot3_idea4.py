# ...existing code...
from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    payload = request.args.get('payload')
    if not payload or not isinstance(payload, str):
        return jsonify({'error': 'payload required'}), 400
    if len(payload) > 2000:
        return jsonify({'error': 'payload too large'}), 400
    try:
        data = yaml.safe_load(payload)
    except Exception:
        return jsonify({'error': 'invalid payload'}), 400
    if not isinstance(data, dict):
        return jsonify({'error': 'invalid payload'}), 400
    name = data.get('name')
    if not isinstance(name, str):
        return jsonify({'error': 'invalid payload'}), 400
    name = name.strip()
    if not (1 <= len(name) <= 100):
        return jsonify({'error': 'invalid payload'}), 400
    return jsonify({'name': name})
