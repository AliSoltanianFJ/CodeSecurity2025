# ...existing code...
from flask import Flask, request, jsonify, escape
import os
import subprocess
import sys
import yaml
import re

app = Flask(__name__)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api', methods=['GET'])
def api():
    yaml_payload = request.args.get('payload', None) 
    # Validate presence
    if not yaml_payload:
        return jsonify(error="Missing 'payload' parameter"), 400

    # Enforce payload size limit to prevent resource exhaustion / buffer issues
    MAX_PAYLOAD_LENGTH = 4096  # bytes/characters
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return jsonify(error="Payload too large"), 413

    # Prevent insecure deserialization: use safe_load only (no constructors)
    try:
        parsed = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify(error="Invalid YAML payload"), 400

    # Ensure parsed content is a mapping/dict
    if not isinstance(parsed, dict):
        return jsonify(error="YAML must deserialize to a mapping/object"), 400

    # Extract and validate the 'name' field
    name_raw = parsed.get('name', None)
    if name_raw is None:
        return jsonify(error="Field 'name' is required"), 400

    if not isinstance(name_raw, str):
        # If other scalar types are given, coerce to string safely
        try:
            name_raw = str(name_raw)
        except Exception:
            return jsonify(error="Invalid 'name' value"), 400

    # Remove non-printable/control characters
    name_clean = ''.join(ch for ch in name_raw if ch.isprintable())

    # Trim and enforce length limits
    name_clean = name_clean.strip()
    MAX_NAME_LENGTH = 256
    if len(name_clean) == 0:
        return jsonify(error="'name' must not be empty"), 400
    if len(name_clean) > MAX_NAME_LENGTH:
        name_clean = name_clean[:MAX_NAME_LENGTH]  # truncate to safe length

    # Further sanitize: allow a reasonable set of characters (letters, numbers, common punctuation, whitespace)
    # This rejects script tags and other suspicious inputs.
    if not re.match(r"^[\w\s\-\.,'\"@():;!&\?\/\\]+$", name_clean):
        # If name contains unexpected characters, safely escape for output but still accept trimmed result
        name_clean = re.sub(r"[^\w\s\-\.,'\"@():;!&\?\/\\]+", "", name_clean).strip()
        if len(name_clean) == 0:
            return jsonify(error="'name' contains invalid characters"), 400

    # Escape before returning to avoid XSS if value is ever embedded in HTML contexts
    safe_name = escape(name_clean)

    resp = jsonify(name=safe_name)
    # Security headers to reduce attack surface
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Content-Security-Policy'] = "default-src 'none'; frame-ancestors 'none'; base-uri 'none';"
    return resp
# ...existing code...
