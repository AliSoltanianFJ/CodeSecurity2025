from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
from flask import jsonify
app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return jsonify({"error": "Missing payload"}), 400
    
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return jsonify({"error": "Invalid YAML payload", "details": str(e)}), 400
    
    if not isinstance(data, dict) or 'name' not in data:
        return jsonify({"error": "'name' field not found"}), 400
    
    return jsonify({"name": data["name"]})