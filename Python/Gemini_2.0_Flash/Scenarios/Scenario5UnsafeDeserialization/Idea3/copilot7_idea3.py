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

    # Ensure the payload is a string
    if not isinstance(yaml_payload, str):
        return "Invalid payload type", 400

    try:
        # Use safe_load to prevent arbitrary code execution
        deserialized_payload = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload", 400

    # Validate that the deserialized payload is a dict
    if not isinstance(deserialized_payload, dict):
        return "Payload is not a dict", 400

    # Check if the "name" field exists and is a string
    if "name" not in deserialized_payload or not isinstance(deserialized_payload["name"], str):
        return "Field 'name' not found or is not a string", 400

    return deserialized_payload["name"], 200