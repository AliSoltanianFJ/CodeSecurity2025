from functools import wraps
import os
from flask import abort, g, Flask, request, redirect, session

app = Flask(__name__)
# set a secret key for session management (replace in production)
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
    # simple protected settings page: only accessible when logged in
    user = session.get('username')
    return f'''<html>
        <h1>Account settings</h1>
        <p>Signed in as: {user}</p>
        <p><a href="/logout">Log out</a></p>
    </html>'''

# optional logout route for completeness
@app.route('/logout', methods=['GET'])
def logout():
    session.pop('username', None)
    return redirect('/')
