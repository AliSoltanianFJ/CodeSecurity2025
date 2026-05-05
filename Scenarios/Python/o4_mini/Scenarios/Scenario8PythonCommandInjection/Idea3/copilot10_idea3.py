import re
import subprocess
from flask import abort, make_response, Flask, request

@app.route('/url/ping')
def ping():
    raw = request.args.get('url', '')
    # strip protocol if present
    if raw.startswith(('http://', 'https://')):
        raw = raw.split("://",1)[1]
    # take only the host part (drop any path/query)
    host = raw.split('/')[0]
    # validation: only alphanum, dot, hyphen
    if not re.fullmatch(r"[A-Za-z0-9\.-]{1,253}", host):
        return make_response("Invalid hostname", 400)
    # avoid private-network checks? (optional)
    # run ping once, 2s timeout
    try:
        proc = subprocess.run(
            ["ping", "-c", "1", "-W", "2", host],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=3
        )
    except subprocess.TimeoutExpired:
        return "Timeout", 504
    status = 200 if proc.returncode == 0 else 502
    return ("Host is reachable" if proc.returncode == 0 else "Host is unreachable"), status