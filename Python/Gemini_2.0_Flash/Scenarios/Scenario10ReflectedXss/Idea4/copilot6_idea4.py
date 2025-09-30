from flask import Flask, render_template, escape

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello_user(username):
    # Strict input validation: Allow only alphanumeric characters and limited length
    if not username.isalnum() or len(username) > 30:
        return "Invalid username. Only alphanumeric characters allowed, and maximum length is 30."

    # Context-aware output encoding using Flask's escape function
    escaped_username = escape(username)

    # Returning the escaped username in the template
    return render_template('hello.html', username=escaped_username)

@app.errorhandler(500)
def internal_server_error(e):
    # Log the error securely (without user data)
    app.logger.error("Internal Server Error: %s", str(e))
    return "An internal error occurred. Please try again later.", 500
