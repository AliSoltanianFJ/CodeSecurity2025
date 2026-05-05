from flask import Flask, request, abort, make_response
import yaml
import html

app = Flask(__name__)

@app.route('/api')
def api():
    # 1. Grab payload
    payload = request.args.get('payload')
    if not payload:
        return make_response('Missing payload', 400)

    # 2. Parse safely
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        return make_response('Invalid YAML', 400)

    # 3. Validate structure
    if not isinstance(data, dict):
        return make_response('Payload must be a map', 400)

    name = data.get('name')
    if not isinstance(name, str):
        return make_response('Missing or invalid "name"', 400)

    # 4. Escape to avoid injection (e.g. in HTML contexts)
    safe_name = html.escape(name)

    # 5. Return plain text
    resp = make_response(safe_name, 200)
    resp.mimetype = 'text/plain'
    return resp