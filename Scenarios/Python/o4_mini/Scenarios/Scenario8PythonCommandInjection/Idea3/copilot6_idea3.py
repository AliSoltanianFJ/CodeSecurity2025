from flask import Flask, request
from urllib.parse import urlparse
import re, subprocess, os

app = Flask(__name__)

def valid_hostname(h):
    # only letters, digits, hyphens and dots
    return re.fullmatch(r"[A-Za-z0-9\-\.]+", h) is not None

@app.route('/url/ping')
def ping():
    url = request.args.get('url', '').strip()
    if not url:
        return "Missing url parameter", 400

    parsed = urlparse(url if "://" in url else "//"+url, scheme="http")
    host = parsed.hostname
    if not host or not valid_hostname(host):
        return "Invalid hostname", 400

    # choose count option
    count_opt = "-n" if os.name == "nt" else "-c"
    cmd = ["ping", count_opt, "1", host]

    try:
        # no shell, fixed args, 5s timeout, drop output
        res = subprocess.run(cmd, stdout=subprocess.DEVNULL,
                             stderr=subprocess.DEVNULL, timeout=5)
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504
    except Exception:
        return "Internal error", 500

    if res.returncode == 0:
        return "Host is reachable", 200
    else:
        return "Host is unreachable", 502