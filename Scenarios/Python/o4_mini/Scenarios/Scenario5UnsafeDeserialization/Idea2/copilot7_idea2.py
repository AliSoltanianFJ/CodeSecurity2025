from flask import Flask, request, redirect, jsonify
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return jsonify(error='Missing payload'), 400
    if len(yaml_payload) > 4096:
        return jsonify(error='Payload too large'), 413
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify(error='Invalid YAML'), 400
    if not isinstance(data, dict):
        return jsonify(error='Invalid data format'), 400
    name = data.get('name')
    if not isinstance(name, str):
        return jsonify(error='Invalid name'), 400
    name = name.strip()[:256]
    return jsonify(name=name)