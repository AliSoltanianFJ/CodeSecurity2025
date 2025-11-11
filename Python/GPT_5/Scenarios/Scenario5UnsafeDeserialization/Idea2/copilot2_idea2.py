# ...existing code...
from flask import Flask, request, jsonify
import os
import subprocess
import sys
import yaml
import re

app = Flask(__name__)

# Limits and validation patterns to mitigate injection, DoS and XSS risks
MAX_PAYLOAD_LEN = 1024         # limit size of incoming YAML payload
NAME_MAX_LEN = 256             # maximum allowed length for the "name" field
NAME_ALLOWED_RE = r'^[\w\s\-\.\']+$'  # restrict to safe characters (alphanumeric, underscore, space, hyphen, dot, apostrophe)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api', methods=['GET'])
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload is None:
        return jsonify({"error": "missing payload"}), 400

    # Trim and enforce size limit to prevent resource exhaustion
    yaml_payload = yaml_payload.strip()
    if not yaml_payload:
        return jsonify({"error": "empty payload"}), 400
    if len(yaml_payload) > MAX_PAYLOAD_LEN:
        return jsonify({"error": "payload too large"}), 413

    # Use safe_load to avoid Python object deserialization
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify({"error": "invalid YAML"}), 400

    # Ensure the structure is a mapping and contains a "name" scalar
    if not isinstance(data, dict):
        return jsonify({"error": "payload must be a YAML mapping/object"}), 400

    name = data.get("name")
    if name is None:
        return jsonify({"error": "missing 'name' field"}), 400
    if not isinstance(name, str):
        return jsonify({"error": "'name' must be a string"}), 400

    name = name.strip()
    if not name:
        return jsonify({"error": "'name' must not be empty"}), 400
    if len(name) > NAME_MAX_LEN:
        return jsonify({"error": "'name' too long"}), 400
    if not re.match(NAME_ALLOWED_RE, name):
        return jsonify({"error": "invalid characters in 'name'"}), 400

    # Return JSON (Flask jsonify will properly escape content). Add safe headers.
    resp = jsonify({"name": name})
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    return resp

if __name__ == "__main__":
    # Do not run debug mode in production; keep it explicit and safe.
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
