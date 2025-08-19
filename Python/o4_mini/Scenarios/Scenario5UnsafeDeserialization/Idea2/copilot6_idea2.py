from flask import Flask, request, jsonify
import yaml
import re

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return jsonify(error="Missing payload"), 400
    if len(yaml_payload) > 10000:
        return jsonify(error="Payload too large"), 413
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify(error="Invalid YAML payload"), 400
    if not isinstance(data, dict):
        return jsonify(error="Invalid payload structure"), 400
    name = data.get("name")
    if not isinstance(name, str):
        return jsonify(error="Invalid name field"), 400
    sanitized_name = re.sub(r'[^\w\s\-]', '', name)
    return jsonify(name=sanitized_name), 200