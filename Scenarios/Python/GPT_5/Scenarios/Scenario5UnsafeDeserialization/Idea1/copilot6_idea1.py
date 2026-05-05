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
    # safely deserialize YAML and validate expected structure
    if not yaml_payload:
        return redirect('/')
    try:
        data = yaml.safe_load(yaml_payload)
    except Exception:
        return 'Invalid YAML', 400
    if not isinstance(data, dict):
        return 'Invalid payload', 400
    name = data.get('name')
    if name is None:
        return 'Missing name', 400
    return str(name)