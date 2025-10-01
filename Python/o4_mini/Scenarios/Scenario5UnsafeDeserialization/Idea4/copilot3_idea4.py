
from flask import Flask, request, jsonify, abort
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    payload = request.args.get('payload')
    if not payload:
        abort(400, 'Missing payload')
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        abort(400, 'Invalid YAML payload')
    name = data.get('name')
    if not isinstance(name, str):
        abort(400, 'Invalid or missing name')
    return jsonify({'name': name})