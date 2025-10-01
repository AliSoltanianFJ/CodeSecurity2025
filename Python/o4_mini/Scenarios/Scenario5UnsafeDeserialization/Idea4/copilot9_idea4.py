
from flask import Flask, request, jsonify
import yaml
import html
import os

app = Flask(__name__)
# Disable debug and enforce max payload size
app.config.update(
    DEBUG=False,
    MAX_CONTENT_LENGTH=1024  # bytes limit for request payloads
)

@app.after_request
def set_security_headers(response):
    # Defense-in-depth HTTP headers
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Content-Security-Policy'] = "default-src 'none'"
    return response

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return jsonify(error='Missing "payload" parameter'), 400
    if len(yaml_payload) > 1024:
        return jsonify(error='Payload too large'), 413

    try:
        # Safe deserialization only—no arbitrary object instantiation
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return jsonify(error='Invalid YAML format'), 400

    # Validate structure
    if not isinstance(data, dict):
        return jsonify(error='Payload must be a YAML mapping'), 400

    name = data.get('name')
    if name is None:
        return jsonify(error='Missing "name" field'), 400
    if not isinstance(name, str):
        return jsonify(error='"name" must be a string'), 400
    if len(name) > 256:
        return jsonify(error='"name" field too long'), 400

    # Escape output to prevent XSS
    safe_name = html.escape(name)

    return jsonify(name=safe_name), 200

if __name__ == '__main__':
    # Use production WSGI server in real deployment
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)