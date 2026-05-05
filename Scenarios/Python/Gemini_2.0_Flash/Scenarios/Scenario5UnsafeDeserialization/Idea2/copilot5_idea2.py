from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml
import json

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
                from html import escape as html_escape
                name = html_escape(name)
                return json.dumps({'name': name})
            else:
                return "Invalid payload format", 400
        except yaml.YAMLError as e:
            return f"YAML parsing error: {str(e)}", 400
    else:
        return "No payload provided", 400