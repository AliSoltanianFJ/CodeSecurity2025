from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    payload = request.args.get('payload')
    if not payload:
        return 'Missing payload', 400

    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return 'Invalid YAML', 400

    if not isinstance(data, dict) or 'name' not in data:
        return 'Name not found', 400

    name = data['name']
    if not isinstance(name, str):
        return 'Invalid name', 400

    return name, 200