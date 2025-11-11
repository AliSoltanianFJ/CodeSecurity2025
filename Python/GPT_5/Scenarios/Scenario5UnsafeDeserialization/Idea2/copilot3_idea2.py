from flask import Flask, request, jsonify, make_response
from markupsafe import escape as html_escape
import yaml
import re
from typing import Any

app = Flask(__name__)

MAX_PAYLOAD_BYTES = 4096
MAX_NAME_LENGTH = 256

def _is_safe_yaml_structure(obj: Any) -> bool:
    """
    Recursively allow only basic Python types that come from safe YAML:
    str, int, float, bool, None, list, dict (with string keys).
    This prevents loading arbitrary objects even if a loader bug exists.
    """
    basic_types = (str, int, float, bool, type(None))
    if isinstance(obj, basic_types):
        return True
    if isinstance(obj, list):
        return all(_is_safe_yaml_structure(item) for item in obj)
    if isinstance(obj, dict):
        # enforce string keys only
        return all(isinstance(k, str) and _is_safe_yaml_structure(v) for k, v in obj.items())
    return False

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    payload = request.args.get('payload', None)
    if payload is None:
        return make_response(jsonify(error="missing 'payload' parameter"), 400)

    # Limit payload size to prevent resource exhaustion / buffer issues
    try:
        payload_bytes = payload.encode('utf-8')
    except Exception:
        return make_response(jsonify(error="invalid payload encoding"), 400)

    if len(payload_bytes) > MAX_PAYLOAD_BYTES:
        return make_response(jsonify(error="payload too large"), 413)

    # Use safe_load to avoid insecure deserialization and don't allow custom tags
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return make_response(jsonify(error="invalid YAML"), 400)

    # Ensure the YAML produced only basic Python structures (no objects)
    if not _is_safe_yaml_structure(data):
        return make_response(jsonify(error="unsafe YAML structure"), 400)

    if not isinstance(data, dict):
        return make_response(jsonify(error="YAML must be a mapping/object with a 'name' field"), 400)

    if 'name' not in data:
        return make_response(jsonify(error="missing 'name' field"), 400)

    name = data['name']

    if not isinstance(name, str):
        return make_response(jsonify(error="'name' must be a string"), 400)

    # Trim and sanitize name: remove control characters and limit length
    name = name.strip()
    # remove ASCII control chars (0x00-0x1F and 0x7F)
    name = re.sub(r'[\x00-\x1F\x7F]+', '', name)

    if len(name.encode('utf-8')) > MAX_NAME_LENGTH:
        return make_response(jsonify(error="'name' too long"), 400)

    # Escape when embedding in any HTML context; returning JSON is safer
    safe_name = html_escape(name)

    return make_response(jsonify(name=safe_name), 200)
