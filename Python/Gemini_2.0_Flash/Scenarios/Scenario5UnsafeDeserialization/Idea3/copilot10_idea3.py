from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
from flask import jsonify

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return jsonify({"error": "No payload provided"}), 400

    try:
        deserialized_payload = yaml.safe_load(yaml_payload)
    except Exception:
        return jsonify({"error": "Failed to deserialize YAML"}), 400

    if not isinstance(deserialized_payload, dict):
        return jsonify({"error": "Payload must be a dictionary"}), 400

    name_value = deserialized_payload.get("name")
    if name_value is None:
        return jsonify({"error": "Field 'name' not found in payload"}), 400

    if not isinstance(name_value, str):
        return jsonify({"error": "Field 'name' must be a string"}), 400

    return jsonify({"name": name_value}), 200