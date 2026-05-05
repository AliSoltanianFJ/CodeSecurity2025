# ...existing code...
from flask import Flask, request, redirect, jsonify, abort
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None) 
    # ...existing code...
    if not yaml_payload:
        return ("Missing payload parameter 'payload'", 400)

    try:
        # Use safe_load to avoid unsafe deserialization of arbitrary Python objects
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return ("Invalid YAML payload", 400)

    if not isinstance(data, dict):
        return ("YAML payload must be a mapping containing a 'name' field", 400)

    name = data.get("name")
    if name is None:
        return ("Missing 'name' field in YAML payload", 400)

    # return the name as plain text
    return str(name), 200
# ...existing code...
