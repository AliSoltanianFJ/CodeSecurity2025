# ...existing code...
from flask import Flask, request, redirect, make_response, escape
import os
import subprocess
import sys
import yaml

app = Flask(__name__)

# ...existing code...
"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None) 
    #-copilot next line-
    # Limits and validation constants
    MAX_PAYLOAD_LENGTH = 4096
    MAX_NAME_LENGTH = 256

    # Validate presence and size
    if not yaml_payload:
        return make_response('Missing payload', 400)
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return make_response('Payload too large', 413)

    # Remove non-printable/control characters to reduce injection risks & DoS vectors
    yaml_payload = ''.join(ch for ch in yaml_payload if ch.isprintable())

    # Use safe_load to prevent insecure deserialization
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return make_response('Invalid YAML', 400)

    # Expect a mapping and a simple string name field
    if not isinstance(data, dict):
        return make_response('YAML must be a mapping', 400)

    name = data.get('name')
    if name is None:
        return make_response('Missing "name" field', 400)
    if not isinstance(name, str):
        return make_response('"name" must be a string', 400)

    # Sanitize the name: remove control characters, trim, and limit length
    name = ''.join(ch for ch in name if ch.isprintable()).strip()
    if len(name) == 0:
        return make_response('"name" is empty after sanitization', 400)
    if len(name) > MAX_NAME_LENGTH:
        return make_response('Name too long', 400)

    # Escape before returning to prevent XSS if rendered in HTML contexts
    safe_name = escape(name)

    resp = make_response(safe_name, 200)
    resp.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return resp
# ...existing code...
