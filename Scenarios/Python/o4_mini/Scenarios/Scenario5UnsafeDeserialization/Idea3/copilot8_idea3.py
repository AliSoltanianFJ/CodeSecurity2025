from flask import Flask, request, abort, make_response
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return make_response('Missing payload', 400)

    # reject huge inputs
    if len(yaml_payload) > 10_000:
        return make_response('Payload too large', 413)

    # safe deserialization only
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return make_response('Invalid YAML', 400)

    # ensure we got a dict with a string “name”
    name = data.get('name') if isinstance(data, dict) else None
    if not isinstance(name, str):
        return make_response('Invalid name field', 400)

    # return plain text
    resp = make_response(name, 200)
    resp.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return resp