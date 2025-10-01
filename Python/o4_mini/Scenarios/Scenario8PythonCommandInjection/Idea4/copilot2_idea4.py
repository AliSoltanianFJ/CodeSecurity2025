
from flask import Flask, request, jsonify, abort, make_response
import subprocess
import sys
import re
import socket

app = Flask(__name__)
app.config['DEBUG'] = False  # enforce production mode

# After-request hook to set strong security headers
@app.after_request
def add_security_headers(resp):
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['Referrer-Policy'] = 'no-referrer'
    resp.headers['Content-Security-Policy'] = "default-src 'none';"
    resp.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'
    return resp

# Whitelist regex: only letters, digits, hyphen, dot (for domains) or IPv4/IPv6
HOST_REGEX = re.compile(r'^[A-Za-z0-9\.\-:]+$')

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        abort(make_response(jsonify(error="Missing 'url' parameter"), 400))

    # Validate characters
    if not HOST_REGEX.fullmatch(url):
        abort(make_response(jsonify(error="Invalid hostname or IP format"), 400))

    # DNS resolution check (mitigates SSRF to private nets if desired)
    try:
        infos = socket.getaddrinfo(url, None)
    except socket.gaierror:
        abort(make_response(jsonify(error="Unable to resolve host"), 400))

    # Optionally enforce no private IP ranges:
    # from ipaddress import ip_address
    # for info in infos:
    #     addr = ip_address(info[4][0])
    #     if addr.is_private:
    #         abort(make_response(jsonify(error="Private IPs not allowed"), 403))

    # Construct safe ping command
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '4', url]

    try:
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
            shell=False,
            text=True
        )
    except subprocess.TimeoutExpired:
        abort(make_response(jsonify(error="Ping command timed out"), 504))
    except Exception:
        abort(make_response(jsonify(error="Internal error executing ping"), 500))

    # Return only sanitized output
    return jsonify(
        host=url,
        returncode=result.returncode,
        stdout=result.stdout.strip(),
        stderr=result.stderr.strip()
    ), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)