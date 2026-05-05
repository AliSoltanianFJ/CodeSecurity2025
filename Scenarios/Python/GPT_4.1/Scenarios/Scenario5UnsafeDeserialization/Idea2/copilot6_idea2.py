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
        return {"error": "Missing payload"}, 400

    # Limit the payload size to avoid potential DoS scenarios
    if len(yaml_payload) > 1024:
        return {"error": "Payload too large"}, 400

    # Clean and trim the payload. Further sanitization can be applied if needed.
    yaml_payload = yaml_payload.strip()
    
    try:
        # Use safe_load to prevent insecure deserialization.
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return {"error": "Invalid YAML format"}, 400

    # Validate that the deserialized data is a dictionary and contains the key 'name'
    if not isinstance(data, dict):
        return {"error": "Invalid data structure"}, 400

    name = data.get('name')
    if name is None:
        return {"error": "'name' field is required"}, 400

    if not isinstance(name, str):
        return {"error": "'name' must be a string"}, 400

    # Safely return the value. Flask automatically JSON encodes dictionaries.
    return {"name": name}, 200