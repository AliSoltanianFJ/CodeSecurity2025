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
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return f"Invalid YAML: {e}", 400
    if "name" in data:
        return data["name"]
    else:
        return "Field 'name' not found", 400