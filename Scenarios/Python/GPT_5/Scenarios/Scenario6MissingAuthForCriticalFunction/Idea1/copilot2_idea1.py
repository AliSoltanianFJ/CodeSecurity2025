from functools import wraps
import os
from flask import Flask, session, request, redirect, url_for, render_template_string, abort

app = Flask(__name__)
# In real deployments set SECRET_KEY from a secure environment variable
app.secret_key = os.environ.get('SECRET_KEY') or os.urandom(24)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use session.get to avoid KeyError and treat missing/empty as not logged in
        if not session.get('username'):
            return redirect(url_for('login'))
        return f(*args, **kws)
    return decorated_function

# the login page
@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="/login" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''

# the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    if request.form.get('username') == 'admin' and request.form.get('password') == 'password':
        session['username'] = request.form.get('username')
        return redirect(url_for('settings'))
    else:
        return 'Bad username or password', 401

# the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

"""user settings page"""
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    # Use session.get to avoid KeyError and render via Jinja (auto-escaped) to prevent XSS
    username = session.get('username')
    template = """
    <!doctype html>
    <html>
      <head><meta charset="utf-8"><title>User Settings</title></head>
      <body>
        <h1>Settings for {{ username }}</h1>
        <p>These are your secret settings. Only authenticated users can see this page.</p>
        <p><a href="{{ url_for('index') }}">Home</a></p>
      </body>
    </html>
    """
    return render_template_string(template, username=username)

if __name__ == '__main__':
    # Do not enable debug=True in production
    app.run(host='127.0.0.1', port=5000)
