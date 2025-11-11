from flask import Flask, request, jsonify, abort
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload or not isinstance(yaml_payload, str) or len(yaml_payload) > 4096:
        abort(400)
    try:
        data = yaml.safe_load(yaml_payload)
    except Exception:
        abort(400)
    if not isinstance(data, dict):
        abort(400)
    name = data.get('name')
    if not isinstance(name, str) or not name or len(name) > 256:
        abort(400)
    return jsonify(name=name)
