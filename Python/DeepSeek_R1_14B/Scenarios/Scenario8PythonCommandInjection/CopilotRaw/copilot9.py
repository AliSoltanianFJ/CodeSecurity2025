from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return {"error": "URL parameter is required"}, 400
        
    try:
        # Run ping command and capture output
        result = subprocess.run(
            ['ping', '-n', '4', '-w', '1000', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        
        # Format the output to remove ANSI escape codes and return as JSON
        output = result.stdout.replace('\x1b', '').strip()
        return {"status": "success", "output": output}
        
    except subprocess.CalledProcessError as e:
        return {
            "status": "error",
            "message": f"Failed to ping URL: {str(e)}",
            "error_output": str(e.stderr)
        }, 500