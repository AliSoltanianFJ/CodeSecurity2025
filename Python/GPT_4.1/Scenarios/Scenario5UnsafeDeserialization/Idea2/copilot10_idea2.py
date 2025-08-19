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
        return "No payload provided.", 400

    # Limit the payload size to avoid resource exhaustion
    if len(yaml_payload) > 10000:
        return "Payload too large.", 400

    try:
        # Use safe_load to prevent insecure deserialization
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML payload.", 400

    # Validate deserialized structure
    if not isinstance(data, dict):
        return "Invalid payload structure.", 400

    name = data.get("name")
    if name is None:
        return "Field 'name' is missing.", 400

    # Return the name safely (as plain text to prevent XSS)
    return str(name)