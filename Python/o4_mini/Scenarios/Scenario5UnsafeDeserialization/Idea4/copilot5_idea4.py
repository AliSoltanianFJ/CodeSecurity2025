
from flask import Flask, request, Response
from markupsafe import escape
import yaml

app = Flask(__name__)
# Enforce secure defaults
app.config.update(
    DEBUG=False,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax',
    SESSION_COOKIE_SECURE=True,
)

# Maximum allowed payload length to mitigate DoS
MAX_PAYLOAD_LENGTH = 2048

@app.route('/api')
def api():
    # 1. Input validation
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return Response("Missing 'payload' parameter", status=400)
    if len(yaml_payload) > MAX_PAYLOAD_LENGTH:
        return Response("Payload too large", status=413)

    # 2. Safe deserialization
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return Response("Malformed YAML", status=400)

    # 3. Strict schema validation
    if not isinstance(data, dict):
        return Response("Invalid payload structure", status=400)
    name = data.get('name')
    if not isinstance(name, str) or not name:
        return Response("Missing or invalid 'name' field", status=400)

    # 4. Output encoding to prevent XSS
    safe_name = escape(name)
    return Response(safe_name, mimetype='text/plain; charset=utf-8')

if __name__ == '__main__':
    # Bind to localhost only, port configurable via env, enforce TLS in production
    app.run(host='127.0.0.1', port=int(__import__('os').environ.get('PORT', 5000)))