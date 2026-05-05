
from flask import Flask, request, abort, escape
import yaml

app = Flask(__name__)
app.config['DEBUG'] = False  # ensure debug mode is off in production

# strict size limits to prevent resource exhaustion
MAX_PAYLOAD_SIZE = 4096
MAX_NAME_LENGTH = 256

@app.route('/api')
def api():
    # 1. Retrieve raw payload
    raw = request.args.get('payload')
    if raw is None:
        abort(400, description="Missing 'payload' parameter")

    # 2. Enforce maximum payload length
    if len(raw) > MAX_PAYLOAD_SIZE:
        abort(413, description="Payload too large")

    # 3. Safe YAML parsing
    try:
        data = yaml.safe_load(raw)
    except yaml.YAMLError:
        abort(400, description="Invalid YAML payload")

    # 4. Validate structure
    if not isinstance(data, dict):
        abort(400, description="YAML root must be a mapping")

    # 5. Extract and validate 'name'
    name = data.get('name')
    if not isinstance(name, str):
        abort(400, description="Field 'name' must be a string")
    if len(name) > MAX_NAME_LENGTH:
        abort(400, description="Field 'name' too long")

    # 6. Escape output to guard against HTML injection
    safe_name = escape(name)

    return safe_name, 200