from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)

# ...existing code...

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not isinstance(yaml_payload, str) or not yaml_payload.strip():
        return jsonify({'error': 'invalid request'}), 400
    if len(yaml_payload) > 20000:
        return jsonify({'error': 'payload too large'}), 413
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify({'error': 'invalid request'}), 400
    if not isinstance(data, dict):
        return jsonify({'error': 'invalid request'}), 400
    name = data.get('name')
    if not isinstance(name, str):
        return jsonify({'error': 'invalid request'}), 400
    name = name.strip()
    if not name or len(name) > 256:
        return jsonify({'error': 'invalid request'}), 400
    return jsonify({'name': name})
