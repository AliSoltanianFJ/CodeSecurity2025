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
    # Use safe_load to avoid executing arbitrary Python objects
    if yaml_payload is None:
        return 'Missing payload parameter', 400
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return 'Invalid YAML', 400

    if not isinstance(data, dict):
        return 'YAML payload must be a mapping', 400

    name = data.get('name')
    if name is None:
        return 'Field "name" not found', 400
    if not isinstance(name, str):
        return 'Field "name" must be a string', 400

    return name, 200
