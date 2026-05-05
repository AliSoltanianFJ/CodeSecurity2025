import re
from flask import Flask, request, abort
import subprocess

app = Flask(__name__)

# simple hostname whitelist: labels 1–63 chars, letters/digits/hyphens, no leading/trailing hyphens
HOSTNAME_RE = re.compile(r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?:\.(?!-)[A-Za-z0-9-]{1,63})*$')

@app.route('/url/ping')
def ping():
    host = request.args.get('url', '').strip()
    if not host:
        abort(400, 'Missing url parameter')
    if not HOSTNAME_RE.match(host):
        abort(400, 'Invalid hostname')

    # build command without shell interpolation
    cmd = ['ping', '-c', '1', host]  # on Windows you might use ['ping','-n','1',host]
    try:
        # suppress output, enforce timeout
        result = subprocess.run(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        # no ICMP reply in time
        return 'Timeout', 504

    # return simple status
    if result.returncode == 0:
        return 'Alive', 200
    else:
        return 'Unreachable', 503