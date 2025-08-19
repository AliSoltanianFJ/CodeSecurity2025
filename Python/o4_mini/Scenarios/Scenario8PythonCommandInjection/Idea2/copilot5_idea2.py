# ...existing code...
from flask import Flask, request, jsonify
import subprocess
import re
import sys
import html

app = Flask(__name__)

@app.route('/ping')
def ping():
    # Fetch and trim URL parameter
    url = request.args.get('url', '').strip()
    if not url:
        return jsonify(error="No URL provided."), 400

    # Validate format: only allow alphanumeric, dots and hyphens
    if not re.fullmatch(r'[A-Za-z0-9.-]+', url):
        return jsonify(error="Invalid URL format."), 400

    # Prevent excessively long input
    if len(url) > 255:
        return jsonify(error="URL too long."), 400

    # Choose correct ping flag per OS
    param = '-n' if sys.platform.startswith('win') else '-c'

    try:
        # Safe subprocess invocation avoids shell injection
        result = subprocess.run(
            ['ping', param, '1', url],
            capture_output=True,
            text=True,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return jsonify(error="Ping command timed out."), 504
    except Exception:
        # Hide internal errors, avoid leaking stack traces
        return jsonify(error="Internal server error."), 500

    # Capture and truncate output to avoid huge buffers
    output = result.stdout if result.returncode == 0 else result.stderr
    safe_output = html.escape(output[:10000])

    status = 200 if result.returncode == 0 else 400
    return f"<pre>{safe_output}</pre>", status

if __name__ == '__main__':
    app.run()
# ...existing code...