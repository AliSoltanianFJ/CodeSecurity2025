import re
from flask import Flask, render_template, abort

app = Flask(__name__)

def sanitize_username(username):
    # Validate and sanitize username input
    # Allow only letters, numbers, underscores, periods, and hyphens
    if not re.match(r'^[\w.-]+$', username) or len(username) > 50:
        return None
    return username

@app.route("/hello/<username>")
def hello_user(username):
    try:
        # Sanitize input to prevent injection attacks
        sanitized_username = sanitize_username(username)
        
        if not sanitized_username:
            abort(400, "Invalid username")
            
        # Use proper HTML escaping in template rendering
        return render_template("hello.html", 
                             username=sanitized_username), 200
        
    except Exception as e:
        # Prevent information disclosure through error messages
        app.logger.error(f"Error processing request: {str(e)}")
        abort(500, "Internal Server Error")