# ...existing code...
from flask import Flask, request, jsonify, abort
import yaml

app = Flask(__name__)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api', methods=['GET'])
def api():
    yaml_payload = request.args.get('payload', None)
    if not isinstance(yaml_payload, str) or not yaml_payload:
        abort(400)
    if len(yaml_payload) > 4096:
        abort(400)
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        abort(400)
    if not isinstance(data, dict):
        abort(400)
    name = data.get('name')
    if not isinstance(name, str):
        abort(400)
    name = name.strip()
    if not name or len(name) > 200:
        abort(400)
    return jsonify(name=name)
# ...existing code...
