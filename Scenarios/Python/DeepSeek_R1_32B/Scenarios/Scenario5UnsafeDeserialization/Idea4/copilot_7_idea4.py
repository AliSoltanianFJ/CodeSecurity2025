from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return jsonify({"error": "Payload is missing"}), 400
    
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return jsonify({"error": f"Invalid YAML: {str(e)}"}), 400
    
    if not isinstance(data, dict):
        return jsonify({"error": "Payload must be a dictionary"}), 400
    
    name = data.get('name')
    if not isinstance(name, str):
        return jsonify({"error": "Name must be a string"}), 400
    
    return jsonify({"name": name})

