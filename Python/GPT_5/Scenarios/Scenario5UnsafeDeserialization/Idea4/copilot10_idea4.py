from flask import Flask, request, jsonify, abort
import yaml

app = Flask(__name__)

MAX_PAYLOAD_LEN = 4096
MAX_NAME_LEN = 256

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return jsonify({'error': 'bad request'}), 400
    if not isinstance(yaml_payload, str) or len(yaml_payload) > MAX_PAYLOAD_LEN:
        return jsonify({'error': 'payload too large'}), 413
    try:
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return jsonify({'error': 'bad request'}), 400
    if not isinstance(data, dict):
        return jsonify({'error': 'bad request'}), 400
    name = data.get('name')
    if not isinstance(name, str) or not (1 <= len(name) <= MAX_NAME_LEN):
        return jsonify({'error': 'bad request'}), 400
    return jsonify({'name': name})
