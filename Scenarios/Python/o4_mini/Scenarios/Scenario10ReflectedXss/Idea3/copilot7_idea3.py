from flask import Flask, escape

app = Flask(__name__)

@app.route("/hello/<username>")
def hello(username):
    # escape() will HTML-escape any special chars in username
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"