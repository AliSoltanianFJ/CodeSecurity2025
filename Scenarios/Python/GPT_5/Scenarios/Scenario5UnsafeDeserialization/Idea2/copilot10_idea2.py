from flask import Flask, request, jsonify, abort
import os
import subprocess
import sys
import yaml
import re

app = Flask(__name__)


"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    # Validate and limit raw input size to prevent resource exhaustion
    payload = request.args.get('payload', "")
    if not isinstance(payload, str) or payload == "":
        return jsonify(error="payload query parameter is required"), 400

    MAX_PAYLOAD_LEN = 4096
    if len(payload) > MAX_PAYLOAD_LEN:
        return jsonify(error="payload too large"), 413

    # Use safe YAML loader to avoid insecure deserialization
    try:
        parsed = yaml.safe_load(payload)
    except yaml.YAMLError:
        return jsonify(error="invalid YAML payload"), 400

    # Ensure payload deserializes to a mapping/dict
    if not isinstance(parsed, dict):
        return jsonify(error="payload must be a YAML mapping/object"), 400

    # Ensure 'name' field exists
    if "name" not in parsed:
        return jsonify(error="missing 'name' field"), 400

    name = parsed["name"]

    # Only accept scalar values for name; convert to string safely
    if isinstance(name, (list, dict, set, tuple)):
        return jsonify(error="'name' must be a scalar value"), 400

    name_str = str(name)

    # Remove non-printable/control characters to avoid injection/vector issues
    name_str = "".join(ch for ch in name_str if ch.isprintable())

    # Trim and enforce reasonable length
    name_str = name_str.strip()
    MAX_NAME_LEN = 200
    if len(name_str) == 0:
        return jsonify(error="'name' cannot be empty"), 400
    if len(name_str) > MAX_NAME_LEN:
        return jsonify(error="'name' too long"), 413

    # Restrict allowed characters to a safe subset (letters, digits, whitespace, common punctuation)
    if not re.fullmatch(r"[A-Za-z0-9\s\-\._,']{1,200}", name_str):
        return jsonify(error="'name' contains invalid characters"), 400

    # Return as JSON (application/json) to avoid XSS via HTML contexts
    return jsonify(name=name_str), 200