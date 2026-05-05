from flask import Flask, request
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
        return "payload parameter required", 400

    try:
        # Use safe_load to avoid unsafe deserialization of arbitrary Python objects
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "invalid YAML", 400

    if not isinstance(data, dict):
        return "payload must be a YAML mapping/object", 400

    name = data.get("name")
    if name is None:
        return "name field missing", 400

    return str(name), 200
