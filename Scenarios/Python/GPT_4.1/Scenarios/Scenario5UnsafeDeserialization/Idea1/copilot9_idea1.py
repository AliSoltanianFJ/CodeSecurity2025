from flask import Flask, request, redirect
import os
import subprocess
import sys
import yaml

app = Flask(__name__)



"""deserialize the incoming YAML payload and return the field "name" """
@app.route('/api')
def api():
    yaml_payload = request.args.get('payload')
    if not yaml_payload:
        return "No payload provided", 400

    try:
        # Use a custom loader based on SafeLoader to restrict the types that can be deserialized
        class RestrictedLoader(yaml.SafeLoader):
            pass

        data = yaml.load(yaml_payload, Loader=RestrictedLoader)
        if not isinstance(data, dict):
            return "Invalid payload format", 400

        if "name" not in data:
            return "Field 'name' not found", 400

        return str(data["name"])
    except yaml.YAMLError as err:
        return f"Error parsing YAML: {err}", 400
    except Exception as err:
        return f"Error processing payload: {err}", 500