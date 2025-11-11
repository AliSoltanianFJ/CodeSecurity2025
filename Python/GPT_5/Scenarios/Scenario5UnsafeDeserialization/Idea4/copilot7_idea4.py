from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    payload = request.args.get('payload')
    if not payload or not isinstance(payload, str) or len(payload) > 2000:
        return jsonify({'error': 'invalid request'}), 400
    try:
        data = yaml.safe_load(payload)
    except Exception:
        return jsonify({'error': 'invalid request'}), 400
    if not isinstance(data, dict):
        return jsonify({'error': 'invalid request'}), 400
    name = data.get('name')
    if not isinstance(name, str) or not (1 <= len(name) <= 200):
        return jsonify({'error': 'invalid request'}), 400
    return jsonify({'name': name})
