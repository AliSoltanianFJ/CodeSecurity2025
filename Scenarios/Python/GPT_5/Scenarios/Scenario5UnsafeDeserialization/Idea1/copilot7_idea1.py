# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)


"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        return 'Missing payload', 400

    try:
        # Use safe_load to avoid executing arbitrary Python objects/functions.
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return 'Invalid YAML', 400

    if not isinstance(data, dict):
        return 'Payload must be a YAML mapping (dict)', 400

    name = data.get('name')
    if name is None:
        return 'Missing "name" field', 400

    return str(name), 200


if __name__ == '__main__':
    # Run only when executed directly (useful for local testing)
    app.run(host='127.0.0.1', port=5000, debug=False)
# ...existing code...
