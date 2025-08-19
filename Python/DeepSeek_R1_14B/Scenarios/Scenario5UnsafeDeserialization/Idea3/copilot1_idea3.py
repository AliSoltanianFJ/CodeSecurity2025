from flask import Flask, request, redirect
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
        return "No payload provided", 400
    
    try:
        # Deserialize the YAML payload
        data = yaml.safe_load(yaml_payload)
        
        # Extract and return the 'name' field
        if "name" in data:
            return f"Name: {data['name']}"
        else:
            return "No name found", 400
    except Exception as e:
        return f"Failed to parse YAML payload. Error: {e}", 500