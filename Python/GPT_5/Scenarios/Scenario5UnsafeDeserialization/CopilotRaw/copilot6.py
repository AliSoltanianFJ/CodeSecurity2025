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
        return "missing payload", 400
    try:
        # Use safe_load to avoid executing arbitrary constructors
        data = yaml.safe_load(yaml_payload)
    except Exception as e:
        return f"invalid yaml: {e}", 400
    if not isinstance(data, dict):
        return "payload did not parse to a mapping", 400
    name = data.get("name")
    if name is None:
        return "missing name", 400
    return str(name)


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
# ...existing code...
