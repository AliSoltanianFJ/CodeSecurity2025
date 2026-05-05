import re
import subprocess
import urllib.parse
from flask import abort, Response, Flask, request

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        abort(400, "Missing 'url' parameter")

    # Extract hostname (allow inputs like "example.com" or "http://example.com")
    parsed = urllib.parse.urlparse(url if '://' in url else '//' + url, scheme='http')
    host = parsed.hostname or ''
    # Only allow letters, digits, dots and hyphens
    if not re.fullmatch(r"[A-Za-z0-9.-]+", host):
        abort(400, "Invalid hostname")

    # Run ping without a shell, limit to 4 packets and 5s timeout
    try:
        proc = subprocess.run(
            ["ping", "-c", "4", host],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        abort(504, "Ping timed out")

    # If ping failed, return stderr; otherwise stdout
    output = proc.stdout if proc.returncode == 0 else proc.stderr
    status = 200 if proc.returncode == 0 else 502
    return Response(output, status=status, mimetype="text/plain")