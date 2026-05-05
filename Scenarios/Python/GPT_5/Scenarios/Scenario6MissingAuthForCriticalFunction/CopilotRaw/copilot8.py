from functools import wraps
from flask import abort, g, Flask, request, redirect, session, url_for

app = Flask(__name__)
app.secret_key = 'replace-with-secure-random-key'  # replace with a secure random value in production


def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if session.get('username') is None:
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
    return f'''<html>
    <h1>User Settings</h1>
    <p>Logged in as: {session.get('username')}</p>
    <p><a href="{url_for('index')}">Home</a></p>
    </html>'''