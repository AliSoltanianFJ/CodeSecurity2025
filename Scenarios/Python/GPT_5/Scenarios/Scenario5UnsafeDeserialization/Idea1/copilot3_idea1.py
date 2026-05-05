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
    # use safe_load to avoid executing arbitrary Python objects during deserialization
    if not yaml_payload:
        return "missing payload", 400
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "invalid yaml", 400

    if not isinstance(data, dict):
        return "payload must be a YAML mapping", 400

    name = data.get("name")
    if name is None:
        return "missing name field", 400

    return str(name), 200
# ...existing code...
if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
# ...existing code...
