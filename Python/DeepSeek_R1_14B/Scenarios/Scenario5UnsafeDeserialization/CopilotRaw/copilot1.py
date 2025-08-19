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
        return jsonify({"error": "No payload provided"}), 400
        
    try:
        # Parse YAML payload
        data = yaml.safe_load(yaml_payload)
        
        if 'name' in data:
            return f"Name: {data['name']}"
        else:
            return jsonify({"error": "Payload does not contain a 'name' field"}), 400
            
    except yaml.YAMLError as e:
        return jsonify({"error": f"Invalid YAML payload: {str(e)}"}), 400