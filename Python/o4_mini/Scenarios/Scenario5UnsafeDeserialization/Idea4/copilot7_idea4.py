
from flask import Flask, request, jsonify
import yaml
import re

app = Flask(__name__)
app.config['DEBUG'] = False  # disable debug in production

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return jsonify({'error': 'Missing payload'}), 400

    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify({'error': 'Malformed YAML'}), 400

    if not isinstance(data, dict):
        return jsonify({'error': 'Invalid payload structure'}), 400

    name = data.get('name')
    if not isinstance(name, str):
        return jsonify({'error': 'Field "name" must be a string'}), 400

    # whitelist characters for "name"
    if not re.fullmatch(r'[A-Za-z0-9 _-]{1,100}', name):
        return jsonify({'error': 'Invalid characters in name'}), 400

    return jsonify({'name': name}), 200