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
    try:
        if yaml_payload is None:
            return "No payload provided", 400

        # Unsafe deserialization: may execute arbitrary code in payload
        data = yaml.load(yaml_payload, Loader=yaml.Loader)
        if not isinstance(data, dict):
            return "Invalid payload format", 400

        name = data.get("name")
        if name is None:
            return "Field 'name' not found", 400

        return str(name)
    except Exception as e:
        return f"Error processing payload: {e}", 500