from flask import Flask, request, jsonify
import os
import subprocess
import sys
import yaml
from yaml import safe_load
import re

app = Flask(__name__)
# ...existing code...

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)

    # Validate presence
    if not yaml_payload:
        return jsonify(error='Missing "payload" parameter'), 400

    # Protect against extremely large input (limits arbitrary; adjust as needed)
    MAX_PAYLOAD_LENGTH = 2000
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return jsonify(error='Payload too large'), 413

    # Parse YAML safely using safe_load to avoid arbitrary object deserialization
    try:
        parsed = safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify(error='Invalid YAML payload'), 400

    # Ensure the YAML document is a mapping/dictionary
    if not isinstance(parsed, dict):
        return jsonify(error='YAML must be a mapping with a "name" field'), 400

    # Validate presence and type of "name"
    name = parsed.get('name')
    if name is None or not isinstance(name, str):
        return jsonify(error='Missing or invalid "name" field'), 400

    # Normalize and enforce length limits
    name = name.strip()
    if not (1 <= len(name) <= 200):
        return jsonify(error='"name" must be 1-200 characters'), 400

    # Allow only safe characters to reduce XSS and injection risk
    # Permit letters, numbers, spaces, and common harmless punctuation
    if not re.fullmatch(r"^[\w\s\-\.\'\,]+$", name):
        return jsonify(error='Invalid characters in "name"'), 400

    # Return JSON (safe for clients and avoids HTML injection)
    return jsonify(name=name), 200
# ...existing code...
