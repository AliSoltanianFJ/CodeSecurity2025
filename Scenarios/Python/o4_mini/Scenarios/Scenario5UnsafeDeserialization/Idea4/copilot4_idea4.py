
from flask import Flask, request, jsonify, make_response
import re
import yaml

app = Flask(__name__)
# Production safety: ensure debug is off
app.config['DEBUG'] = False

@app.route('/api')
def api():
    # 1. Fetch and basic length check
    payload = request.args.get('payload', '')
    if not payload:
        return make_response(jsonify(error="Missing 'payload' parameter"), 400)
    if len(payload) > 2000:
        return make_response(jsonify(error="Payload too large"), 413)

    # 2. Safe YAML parsing
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return make_response(jsonify(error="Invalid YAML"), 400)

    # 3. Strict structure enforcement
    if not isinstance(data, dict) or 'name' not in data:
        return make_response(jsonify(error="YAML must be a mapping with 'name'"), 400)

    name = data['name']
    if not isinstance(name, str):
        return make_response(jsonify(error="'name' must be a string"), 400)

    # 4. Whitelist‐style content validation for ‘name’
    #    Allow letters, digits, space, underscore, hyphen; max length 100
    if not re.fullmatch(r'[A-Za-z0-9 _-]{1,100}', name):
        return make_response(jsonify(error="Invalid characters in 'name'"), 400)

    # 5. Form and send safe JSON response with hardening headers
    response = jsonify(name=name)
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy']     = 'no-referrer'
    return response