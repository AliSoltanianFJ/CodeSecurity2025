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
    if yaml_payload:
        try:
            data = yaml.safe_load(yaml_payload)
            if 'name' in data:
                return data['name']
            else:
                return "Field 'name' not found in YAML payload"
        except yaml.YAMLError as e:
            return f"Error decoding YAML: {str(e)}"
    else:
        return "No YAML payload provided"