from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import json
from html import escape as html_escape

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if yaml_payload:
        try:
            # Use safe_load to prevent arbitrary code execution
            data = yaml.safe_load(yaml_payload)
            if isinstance(data, dict) and 'name' in data:
                name = data['name']
                # Sanitize output to prevent XSS
                name = html_escape(name)
                return json.dumps({'name': name})
            else:
                return "Invalid payload format", 400
        except yaml.YAMLError as e:
            return "Invalid YAML payload", 400
    else:
        return "Missing payload", 400