from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
def api():
    yaml_payload = request.args.get('payload')
    if yaml_payload is None:
        return "Error: missing 'payload' parameter", 400

    try:
        parsed = yaml.safe_load(yaml_payload)
    except yaml.YAMLError:
        return "Error: invalid YAML format", 400

    if not isinstance(parsed, dict):
        return "Error: payload should be a YAML mapping", 400

    name = parsed.get("name")
    if name is None:
        return "Error: 'name' field not found in payload", 400

    return str(name)