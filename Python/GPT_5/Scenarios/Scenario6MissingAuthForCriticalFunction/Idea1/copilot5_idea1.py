from functools import wraps
from markupsafe import escape
from flask import Flask, session, request, redirect, url_for, abort

app = Flask(__name__)
# Replace with a strong secret in real deployments
app.secret_key = 'change-this-to-a-random-secret'

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
            return redirect(url_for('login'))
        return f(*args, **kws)            
    return decorated_function

#the login page
@app.route('/login', methods=['GET'])
def login():
    return '''
        <html><form action="/login" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <p><button type="submit">Submit</button></p>
        </form></html>
    '''
#the login POST system 
@app.route('/login', methods=['POST'])
def do_login():
    if request.form['username'] == 'admin' and request.form['password'] == 'password':
        session['username'] = request.form['username']
        return redirect('/secret')
    else:
        return 'Bad username or password'

#the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

"""user settings page"""
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    # Use session.get and escape user-controlled values to avoid KeyError and XSS.
    username = session.get('username', '')
    safe_username = escape(username)

    # Do not display or embed any sensitive data. Provide a minimal, safe settings page.
    return (
        '<!doctype html>'
        '<html><head><meta charset="utf-8"><title>User Settings</title></head>'
        '<body>'
        f'<h1>Settings for {safe_username}</h1>'
        '<p>Your account preferences are managed here. No sensitive data is displayed.</p>'
        f'<p><a href="{url_for("index")}">Home</a></p>'
        '</body></html>'
    )
