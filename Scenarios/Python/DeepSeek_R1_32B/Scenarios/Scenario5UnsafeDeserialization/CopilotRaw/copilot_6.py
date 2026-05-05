from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)


@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return "No payload provided", 400
    
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return f"Error loading YAML: {e}", 400
    
    name = data.get('name')
    if name is None:
        return "Name not found in payload", 400
    else:
        return str(name), 200

