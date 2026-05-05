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
    # validate presence
    if yaml_payload is None:
        return "Missing 'payload' parameter", 400

    # parse safely to avoid arbitrary object construction
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError as e:
        return f"Invalid YAML: {e}", 400

    if not isinstance(data, dict):
        return "YAML payload did not produce a mapping", 400

    name = data.get('name')
    if name is None:
        return "No 'name' field in payload", 400

    return str(name)


if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0', port=5000)
# ...existing code...
