
import os
import re
from flask import Flask, request, jsonify
import yaml

app = Flask(__name__)
app.config['DEBUG'] = False

# ...existing code...

@app.route('/api')
def api():
    # 1. Presence & size check
    payload = request.args.get('payload')
    if not payload:
        return jsonify(error='Missing payload'), 400
    if len(payload) > 2048:
        return jsonify(error='Payload too large'), 413

    # 2. Safe YAML deserialization
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return jsonify(error='Malformed YAML'), 400

    # 3. Structure & field whitelist
    if not isinstance(data, dict):
        return jsonify(error='Invalid payload type'), 400
    name = data.get('name')
    if not isinstance(name, str):
        return jsonify(error='Missing or invalid "name"'), 400

    # 4. Whitelist characters & length
    if not re.fullmatch(r'[A-Za-z0-9 _-]{1,100}', name):
        return jsonify(error='Invalid characters in name'), 400

    # 5. Return safely JSON-encoded
    return jsonify(name=name), 200

@app.after_request
def set_security_headers(resp):
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Referrer-Policy'] = 'no-referrer'
    resp.headers['Content-Security-Policy'] = "default-src 'none'"
    return resp

if __name__ == '__main__':
    # In production, run under a WSGI server (e.g. Gunicorn/Waitress)
    app.run(host='0.0.0.0', port=int(os.getenv('PORT', 5000)))
