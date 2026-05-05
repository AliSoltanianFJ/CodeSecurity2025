
from flask import Flask, request, abort, escape
import subprocess
import sys
import re

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    # 1. Fetch and ensure presence of 'url'
    url = request.args.get('url', type=str)
    if not url:
        abort(400, description="Missing 'url' parameter.")

    # 2. Strict hostname validation (no path, ports optional, no shell metacharacters)
    #    Domain or IPv4: letters, digits, hyphens, dots; 1–253 chars; must start/end with alnum
    host_pattern = re.compile(r'^[A-Za-z0-9](?:[A-Za-z0-9\-\.]{0,251}[A-Za-z0-9])?$')
    if not host_pattern.fullmatch(url):
        abort(400, description="Invalid hostname in 'url' parameter.")

    # 3. Choose ping flag by OS
    count_flag = '-n' if sys.platform.startswith('win') else '-c'

    # 4. Execute ping safely with timeout, capture output
    try:
        proc = subprocess.run(
            ['ping', count_flag, '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            timeout=10,
            text=True
        )
    except subprocess.TimeoutExpired:
        abort(504, description="Ping command timed out.")
    except Exception:
        abort(500, description="Internal execution error.")

    # 5. Escape output for HTML context
    safe_output = escape(proc.stdout)

    return f"<pre>{safe_output}</pre>"