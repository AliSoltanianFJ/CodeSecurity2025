from flask import Flask, request, redirect, abort, make_response
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    payload = request.args.get('payload', '').strip()
    if not payload:
        abort(400, "Missing payload")

    if len(payload) > 1024:
        abort(413, "Payload too large")

    try:
        data = yaml.safe_load(payload)
    except yaml.YAMLError:
        abort(400, "Invalid YAML")

    if not isinstance(data, dict):
        abort(400, "Expected mapping")

    name = data.get('name')
    if not isinstance(name, str):
        abort(400, "Invalid or missing name")

    # Return as plain text to avoid HTML injection
    resp = make_response(name, 200)
    resp.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return resp