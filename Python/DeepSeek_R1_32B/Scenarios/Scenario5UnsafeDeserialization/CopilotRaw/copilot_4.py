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
        return "No payload provided"
    
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return f"Error parsing YAML: {str(e)}"
    
    name = data.get('name')
    if name is not None:
        return str(name)
    else:
        return "No name found in payload"

