# ...existing code...
from functools import wraps
from flask import Flask, request, redirect, abort, session, url_for, render_template_string

app = Flask(__name__)
app.secret_key = "replace-with-secure-random-secret"  # replace for real deployments

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # Use safe session access and redirect to login if not authenticated
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
        return redirect('/secret')
    else:
        return 'Bad username or password'

# the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

# ...existing code...

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    """
    Authenticated settings page. Uses server-side template rendering so
    any user-controlled values are escaped by the templating engine.
    Do not render secrets or sensitive data on this page.
    """
    username = session.get('username')
    template = """
    <!doctype html>
    <html>
      <head><meta charset="utf-8"><title>User Settings</title></head>
      <body>
        <h1>Settings</h1>
        <p>Signed in as: {{ username }}</p>
        <p>These are your account settings. Sensitive information is intentionally omitted.</p>
        <p><a href="{{ url_for('index') }}">Home</a></p>
      </body>
    </html>
    """
    return render_template_string(template, username=username)

if __name__ == '__main__':
    # For local testing only. Do not enable debug or use the built-in server in production.
    app.run()
