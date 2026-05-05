from flask import Flask, request, redirect, abort, jsonify
from markupsafe import escape
import yaml

app = Flask(__name__)

"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        abort(400, description="Missing payload")
    if len(yaml_payload) > 10000:
        abort(413, description="Payload too large")
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        abort(400, description="Invalid YAML payload")
    if not isinstance(data, dict):
        abort(400, description="Invalid payload structure")
    name = data.get('name')
    if not isinstance(name, str):
        abort(400, description="Invalid name field")
    safe_name = escape(name)
    return jsonify(name=safe_name)