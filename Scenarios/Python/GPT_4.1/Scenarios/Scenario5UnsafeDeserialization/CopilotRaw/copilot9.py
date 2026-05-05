from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return jsonify({"error": "Missing payload"}), 400

    try:
        data = yaml.safe_load(yaml_payload) # Use safe_load to avoid potential security issues
    except yaml.YAMLError as e:
        return jsonify({"error": "Invalid YAML payload", "details": str(e)}), 400

    if not isinstance(data, dict) or 'name' not in data:
        return jsonify({"error": "'name' field not found in payload"}), 400

    return jsonify({"name": data["name"]})