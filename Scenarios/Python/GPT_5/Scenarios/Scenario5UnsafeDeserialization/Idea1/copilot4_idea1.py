# ...existing code...
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
        return 'missing payload', 400

    try:
        # Use safe_load to avoid executing arbitrary constructors
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return 'invalid YAML', 400

    # Expect a mapping/dictionary
    if not isinstance(data, dict):
        return 'payload must be a YAML mapping', 400

    name = data.get('name')
    if name is None:
        return 'name not found', 400

    return str(name)
# ...existing code...
