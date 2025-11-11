# ...existing code...
from flask import Flask, request, make_response, abort
import subprocess
import sys
import re
import ipaddress
from urllib.parse import urlparse

app = Flask(__name__)


"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url', type=str)
    if not url:
        return make_response("Missing required parameter", 400)

    if len(url) > 255:
        return make_response("Invalid parameter", 400)

    parsed = urlparse(url)
    host = parsed.hostname if parsed.scheme and parsed.hostname else url

    try:
        # validate IP
        try:
            ipaddress.ip_address(host)
        except ValueError:
            # validate hostname per RFC-ish allowlist
            hostname_regex = re.compile(
                r'^(?=.{1,255}$)(?!-)[A-Za-z0-9-]{1,63}(?:\.(?!-)[A-Za-z0-9-]{1,63})*$'
            )
            if not hostname_regex.fullmatch(host):
                return make_response("Invalid parameter", 400)

    except Exception:
        return make_response("Invalid parameter", 400)

    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', host]

    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
    except Exception:
        return make_response("Upstream error", 502)

    output = proc.stdout or proc.stderr or ''
    return make_response(output, 200, {'Content-Type': 'text/plain'})
# ...existing code...
