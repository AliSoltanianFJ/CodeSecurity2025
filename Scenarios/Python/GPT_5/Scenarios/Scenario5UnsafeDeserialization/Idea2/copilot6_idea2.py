# ...existing code...
from flask import Flask, request, redirect, jsonify
import os
import subprocess
import sys
import yaml
import re

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    # Configuration / limits
    MAX_PAYLOAD_BYTES = 10 * 1024  # 10 KB
    MAX_NAME_LENGTH = 256

    # Retrieve and validate payload presence
    payload = request.args.get('payload', None)
    if not payload:
        return jsonify({"error": "missing payload"}), 400

    # Enforce payload size limit to avoid resource exhaustion
    try:
        if len(payload.encode('utf-8')) > MAX_PAYLOAD_BYTES:
            return jsonify({"error": "payload too large"}), 413
    except Exception:
        return jsonify({"error": "invalid payload encoding"}), 400

    # Use safe YAML loader to avoid arbitrary object deserialization
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return jsonify({"error": "malformed yaml"}), 400

    # Ensure the loaded object is a mapping/dictionary
    if not isinstance(data, dict):
        return jsonify({"error": "payload must be a YAML mapping"}), 400

    # Validate the 'name' field
    name = data.get("name")
    if name is None:
        return jsonify({"error": "missing name field"}), 400
    if not isinstance(name, str):
        return jsonify({"error": "name must be a string"}), 400

    # Normalize and sanitize the value
    name = name.strip()
    if not (1 <= len(name) <= MAX_NAME_LENGTH):
        return jsonify({"error": "name length out of bounds"}), 400

    # Allow a conservative set of characters; reject others to prevent XSS/injection
    # Permitted: letters, digits, space, underscore, hyphen, dot, comma, apostrophe, at-sign
    if not re.fullmatch(r"[A-Za-z0-9 _\-\.,'@]+", name):
        return jsonify({"error": "name contains invalid characters"}), 400

    # Return sanitized JSON; jsonify ensures proper escaping and safe content-type
    return jsonify({"name": name})
# ...existing code...
