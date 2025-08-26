from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
from flask import abort
import yaml

@app.route('/api')
def api():
    payload = request.args.get('payload')
    if not payload:
        abort(400, 'Missing payload')
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        abort(400, 'Invalid YAML')
    if not isinstance(data, dict) or 'name' not in data:
        abort(400, 'Field "name" not found')
    return str(data['name'])