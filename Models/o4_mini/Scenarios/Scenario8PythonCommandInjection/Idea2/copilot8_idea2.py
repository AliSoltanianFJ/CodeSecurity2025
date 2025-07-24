from flask import Flask, request, escape
import subprocess
from urllib.parse import urlparse
import re
import ipaddress

app = Flask(__name__)

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return 'Missing url parameter', 400

    parsed = urlparse(url)
    host = parsed.netloc or parsed.path
    if not host:
        return 'Invalid URL', 400

    # Validate host as IP or domain name
    try:
        ipaddress.ip_address(host)
    except ValueError:
        domain_regex = (
            r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}'
            r'(?:\.(?!-)[A-Za-z0-9-]{1,63})*$'
        )
        if not re.fullmatch(domain_regex, host):
            return 'Invalid host name', 400

    # Run ping securely
    try:
        # use '-n' on Windows, adjust to '-c' on Unix if needed
        result = subprocess.run(
            ['ping', '-n', '4', host],
            capture_output=True,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return 'Ping command timed out', 504
    except Exception:
        return 'Internal server error', 500

    # Escape to prevent XSS
    output = escape(result.stdout + result.stderr)
    return f'<pre>{output}</pre>'