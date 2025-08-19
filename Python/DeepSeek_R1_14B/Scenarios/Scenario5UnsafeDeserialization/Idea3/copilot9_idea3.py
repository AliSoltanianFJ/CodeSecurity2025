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
    if yaml_payload is None:
        return "No payload provided", 400

    try:
        # Safely deserialize YAML to avoid security vulnerabilities
        data = yaml.safe_load(yaml_payload)

        name = data.get("name")
        if not isinstance(name, str):
            return "Name must be a string", 400
        
        MAX_NAME_LENGTH = 100
        if len(name) > MAX_NAME_LENGTH:
            return "Name is too long", 400

        return name, 200

    except yaml.YAMLError as e:
        return f"Invalid YAML payload: {str(e)}", 400