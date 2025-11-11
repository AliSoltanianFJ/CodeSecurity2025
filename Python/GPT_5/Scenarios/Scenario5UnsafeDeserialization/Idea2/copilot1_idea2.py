# ...existing code...
from flask import Flask, request, jsonify, abort
import yaml
import re

app = Flask(__name__)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api', methods=['GET'])
def api():
    # Validate presence and size of input to avoid resource exhaustion
    payload = request.args.get('payload', None)
    if payload is None:
        return jsonify(error='Missing "payload" parameter'), 400
    max_payload_len = 2000
    if len(payload) > max_payload_len:
        return jsonify(error=f'Payload too large (max {max_payload_len} characters)'), 413

    # Use safe_load to avoid insecure object deserialization
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return jsonify(error='Invalid YAML payload'), 400

    # Ensure we have a mapping and the expected field
    if not isinstance(data, dict):
        return jsonify(error='YAML must be a mapping/object'), 400

    name = data.get('name')
    if name is None:
        return jsonify(error='Missing "name" field'), 400

    # Type and length checks to avoid unexpected types and very large values
    if not isinstance(name, str):
        return jsonify(error='"name" must be a string'), 400
    name = name.strip()
    if len(name) == 0 or len(name) > 100:
        return jsonify(error='"name" has invalid length'), 400

    # Allow only a safe subset of characters to prevent XSS / injection
    # (letters, numbers, whitespace, underscore, hyphen, apostrophe)
    if not re.fullmatch(r"^[\w\s\-\']+$", name, flags=re.UNICODE):
        return jsonify(error='Invalid characters in "name"'), 400

    # Return JSON (Flask's jsonify ensures safe serialization/escaping for responses)
    return jsonify(name=name)

if __name__ == '__main__':
    # Do not run with debug=True in production (avoids exposing sensitive info)
    app.run(host='127.0.0.1', port=5000, debug=False)
# ...existing code...
