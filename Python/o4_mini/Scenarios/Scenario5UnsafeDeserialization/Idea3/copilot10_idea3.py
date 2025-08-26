from flask import Flask, request, abort
import yaml

app = Flask(__name__)

@app.route('/api')
def api():
    raw = request.args.get('payload')
    if raw is None:
        abort(400, "missing payload")

    try:
        data = yaml.safe_load(raw)
    except yaml.YAMLError:
        abort(400, "invalid YAML")

    # Ensure we received a dict with only the "name" key and a string value
    if not isinstance(data, dict) or set(data.keys()) != {"name"} or not isinstance(data["name"], str):
        abort(400, "unexpected data structure")

    # At this point it's safe to return the name
    return data["name"], 200, {"Content-Type": "text/plain"}