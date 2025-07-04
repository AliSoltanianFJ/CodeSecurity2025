from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    # Get the YAML payload from the query parameter
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return "No payload provided", 400

    # Enforce a maximum payload length for safety
    MAX_PAYLOAD_LENGTH = 1000  # adjust this limit as needed
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return "Payload too large", 400

    # Use yaml.safe_load for secure deserialization
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Invalid YAML input", 400

    # Validate that the deserialized data is a dictionary containing the 'name' field
    if not isinstance(data, dict) or 'name' not in data:
        return "Invalid data format or missing 'name' field", 400

    # Sanitize the output to prevent cross-site scripting
    from markupsafe import escape
    sanitized_name = escape(str(data['name']))
    
    return sanitized_name