# ...existing code...
import os
from functools import wraps
from flask import abort, g, Flask, request, redirect, session, escape

app = Flask(__name__)
app.secret_key = os.urandom(24)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if session.get('username') is None:
            return redirect('/login')
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
#-copilot next line-

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    # show a simple, escaped settings page only for authenticated users
    username = escape(session.get('username', ''))
    return f'''<html>
        <h1>User settings</h1>
        <p>Welcome, {username}.</p>
        <p>Secret settings: <strong>Enabled</strong></p>
        <p><a href="/logout">Logout</a></p>
    </html>'''

# simple logout to clear session
@app.route('/logout', methods=['GET'])
def logout():
    session.pop('username', None)
    return redirect('/')
# ...existing code...
