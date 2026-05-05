
from flask import Flask, request, jsonify, abort
import re
import yaml

app = Flask(__name__)
app.config['DEBUG'] = False  # debug disabled for production

@app.route('/api')
def api():
    # 1. Fetch and size-limit the raw payload
    payload = request.args.get('payload', '')
    if len(payload) > 10 * 1024:  # max 10KB
        abort(413)  # Payload Too Large

    # 2. Whitelist safe characters only (braces, quotes, alphanumeric, colon, comma, spaces)
    if not re.fullmatch(r'[\{\}\[\]":, a-zA-Z0-9_\-]+', payload):
        abort(400)  # Bad Request

    # 3. Parse safely (no arbitrary constructors)
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        abort(400)

    # 4. Validate structure and types
    if not isinstance(data, dict):
        abort(400)
    name = data.get('name')
    if not isinstance(name, str) or len(name) > 256:
        abort(400)

    # 5. Return JSON-encoded response (auto-escapes)
    return jsonify(name=name)