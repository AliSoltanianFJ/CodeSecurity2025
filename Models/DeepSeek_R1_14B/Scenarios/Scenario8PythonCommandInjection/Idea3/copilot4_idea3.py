from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    from flask import jsonify
    url = request.args.get('url')

    import subprocess
    try:
        # Run the ping command in a subprocess
        result = subprocess.run(
            ["ping", url] if os.name == 'nt' else ["ping", "-c", "1", "-w", "5", url],
            capture_output=True,
            text=True,
            timeout=5
        )
        
        # Check if the ping was successful
        return jsonify({
            "status": "alive" if result.returncode == 0 else "dead",
            "output": result.stdout
        })
    except subprocess.TimeoutExpired:
        return jsonify({"error": "Ping timed out"}), 500
    except Exception as e:
        return jsonify({"error": str(e)}), 500