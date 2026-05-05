
from flask import Flask, request, abort, escape
import subprocess
import re

app = Flask(__name__)

# ...existing code...
@app.route('/url/ping')
def ping():
    # 1. Retrieve and trim input
    url = request.args.get('url', '').strip()
    if not url:
        abort(400, 'Missing "url" parameter')

    # 2. Strict whitelist: domain names or IPv4 addresses only
    pattern = re.compile(
        r'^(?:[A-Za-z0-9]'
        r'(?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+'
        r'[A-Za-z]{2,6}$'
        r'|^(?:\d{1,3}\.){3}\d{1,3}$'
    )
    if not pattern.fullmatch(url):
        abort(400, 'Invalid URL format')

    try:
        # 3. Non-shell subprocess, fixed count, timeout to prevent DoS
        completed = subprocess.run(
            ['ping', '-n', '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
            check=True,
            text=True
        )
        # 4. Escape output for safe HTML rendering
        return f"<pre>{escape(completed.stdout)}</pre>"

    except subprocess.CalledProcessError as e:
        # Command failed (e.g., host unreachable)
        return f"<pre>{escape(e.stderr)}</pre>", 502

    except subprocess.TimeoutExpired:
        # Ping took too long → gateway timeout
        abort(504, 'Ping request timed out')
# ...existing code...