from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    try:
        # Use subprocess to run ping command
        response = subprocess.run(
            ['ping.exe', '-c', '1', url] if os.name == 'nt' else ['ping', '-c', '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        if response.returncode == 0:
            # Ping was successful
            return redirect(f'/url/ping_result?result=success')
        else:
            # Handle ping failure
            return redirect(f'/url/ping_result?result=failure')
            
    except Exception as e:
        # Log the error and handle gracefully
        print(f"Error pinging URL: {e}", file=sys.stderr)
        return redirect(f'/url/ping_result?result=error')