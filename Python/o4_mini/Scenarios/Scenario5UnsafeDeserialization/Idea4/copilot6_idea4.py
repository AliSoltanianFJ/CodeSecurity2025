
from flask import Flask, request, jsonify, abort
import yaml

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 1024  # limit payload size to 1KB

@app.route('/api', methods=['GET'])
def api():
    payload = request.args.get('payload', '')
    if not payload:
        abort(400, 'Missing payload')
    if len(payload) > app.config['MAX_CONTENT_LENGTH']:
        abort(413, 'Payload too large')
    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        abort(400, 'Invalid YAML payload')
    if not isinstance(data, dict) or 'name' not in data:
        abort(400, 'Missing or invalid "name" field')
    name = data['name']
    if not isinstance(name, str):
        abort(400, '"name" must be a string')
    return jsonify(name=name)