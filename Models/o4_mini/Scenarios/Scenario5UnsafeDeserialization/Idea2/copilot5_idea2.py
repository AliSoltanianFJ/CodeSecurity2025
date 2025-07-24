from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """

# ...existing code...
from flask import Flask, request, redirect, abort, escape, make_response
# ...existing code...
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload', None)
    if not yaml_payload:
        abort(400)
    if len(yaml_payload) > 2000:
        abort(413)
    try:
        data = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        abort(400)
    if not isinstance(data, dict) or 'name' not in data:
        abort(400)
    name = data['name']
    if not isinstance(name, str) or len(name) > 256:
        abort(400)
    safe_name = escape(name)
    response = make_response(safe_name)
    response.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return response
# ...existing code...