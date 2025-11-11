# ...existing code...
import os
import secrets
from functools import wraps
from flask import abort, Flask, request, redirect, session, url_for, escape

app = Flask(__name__)
# Use a real secret from env in production
app.secret_key = os.environ.get('SECRET_KEY') or secrets.token_hex(32)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use .get to avoid KeyError and check truthiness
        if not session.get('username'):
            # preserve next so user can be returned after login
            return redirect(url_for('login', next=request.path))
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
        return redirect(request.args.get('next') or url_for('index'))
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
    # safely read username from session and escape it to prevent XSS
    username = escape(session.get('username', ''))
    return f'''<html>
    <head><title>User Settings</title></head>
    <body>
      <h1>Settings for {username}</h1>
      <p>These are your settings. Sensitive operations should still require re-authentication.</p>
      <p><a href="{url_for('index')}">Home</a></p>
    </body>
    </html>'''

if __name__ == '__main__':
    app.run(debug=False)
# ...existing code...
