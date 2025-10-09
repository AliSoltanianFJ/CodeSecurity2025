from flask import Flask, render_template, escape

app = Flask(__name__)

@app.route("/hello/<username>")
def say_hello(username):
    escaped_username = escape(username)
    return render_template('hello.html', username=escaped_username)

if __name__ == "__main__":
    app.run()

