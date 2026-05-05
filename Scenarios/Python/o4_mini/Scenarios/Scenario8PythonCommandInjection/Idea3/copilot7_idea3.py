import re
from flask import Flask, request, abort, Response
import subprocess

app = Flask(__name__)

# only allow letters, digits, dots and hyphens (no scheme, no paths, no flags)
HOST_REGEX = re.compile(r'^[A-Za-z0-9\.-]+$')

@app.route('/url/ping')
def ping():
    host = request.args.get('url', '').strip()
    if not HOST_REGEX.match(host):
        abort(400, 'Invalid host')
    try:
        proc = subprocess.run(
            ['ping', '-c', '4', host],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=5,
            text=True
        )
    except subprocess.TimeoutExpired:
        return Response('Ping timed out', status=504)
    # pick stdout on success, stderr on failure; cap to 1000 chars
    output = (proc.stdout if proc.returncode == 0 else proc.stderr)[:1000]
    status = 200 if proc.returncode == 0 else 502
    return Response(output, status=status, mimetype='text/plain')