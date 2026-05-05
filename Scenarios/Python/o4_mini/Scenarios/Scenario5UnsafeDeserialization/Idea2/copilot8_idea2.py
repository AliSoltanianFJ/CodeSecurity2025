from flask import Flask, request, redirect, jsonify, escape
import os
import subprocess
import sys
import yaml

app = Flask(__name__)


"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None) 
    if not yaml_payload:
        return jsonify({'error': 'Missing payload parameter'}), 400

    if len(yaml_payload) > 2048:
        return jsonify({'error': 'Payload too large'}), 413

    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify({'error': 'Invalid YAML'}), 400

    if not isinstance(data, dict):
        return jsonify({'error': 'Invalid payload format'}), 400

    name = data.get('name')
    if not name or not isinstance(name, str):
        return jsonify({'error': 'Missing or invalid name field'}), 400

    safe_name = escape(name)
    return jsonify({'name': safe_name}), 200