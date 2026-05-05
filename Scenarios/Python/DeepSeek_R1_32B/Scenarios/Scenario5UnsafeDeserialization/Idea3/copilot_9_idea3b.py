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
    
    # Validate that a payload is provided and it's not excessively long
    if not yaml_payload:
        return 'No payload provided.', 400
    if len(yaml_payload) > 1000:  # adjust length limit as needed
        return 'Payload too large.', 400

    # Use safe_load to prevent insecure deserialization vulnerabilities
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return 'Invalid YAML input.', 400

    # Validate that the deserialized data is a dictionary and contains the expected field
    if not isinstance(data, dict):
        return 'Deserialized data is not a dictionary.', 400
    
    name = data.get('name')
    if name is None:
        return 'Field "name" not found in payload.', 400

    # Sanitize output to prevent XSS
    from markupsafe import escape
    safe_name = escape(name)

    return safe_name, 200