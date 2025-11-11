# ...existing code...
from flask import Flask, request, jsonify
import os
import subprocess
import sys
import yaml
import re
import html

app = Flask(__name__)


"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)

    # Validate presence and type
    if not yaml_payload or not isinstance(yaml_payload, str):
        return jsonify({'error': 'missing or invalid payload'}), 400

    # Prevent excessively large inputs (mitigates memory/buffer issues)
    MAX_PAYLOAD_LEN = 4096
    if len(yaml_payload) > MAX_PAYLOAD_LEN:
        return jsonify({'error': 'payload too large'}), 413

    # Use safe YAML loader to avoid insecure deserialization
    try:
        parsed = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify({'error': 'invalid YAML'}), 400

    # Only accept a mapping and do not expose other fields
    if not isinstance(parsed, dict):
        return jsonify({'error': 'payload must be a mapping/dictionary'}), 400

    name = parsed.get('name')
    if name is None:
        return jsonify({'error': 'missing name field'}), 400

    # Validate name type and length
    if not isinstance(name, str):
        return jsonify({'error': 'name must be a string'}), 400

    MAX_NAME_LEN = 100
    if len(name) > MAX_NAME_LEN:
        return jsonify({'error': 'name too long'}), 413

    # Sanitize: allow a safe subset of characters; escape for extra safety
    # Allowed: letters, numbers, spaces, underscore, hyphen, dot, apostrophe
    if not re.fullmatch(r"^[\w\s\-\.\']*$", name):
        # If disallowed characters are present, escape to avoid XSS and return a sanitized name
        safe_name = html.escape(name)
    else:
        safe_name = html.escape(name)

    # Return only the sanitized name; content is JSON to prevent script execution in browsers
    return jsonify({'name': safe_name}), 200

# ...existing code...
