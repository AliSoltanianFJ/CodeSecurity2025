from functools import wraps
from flask import abort, g, Flask, request, redirect, session, url_for

app = Flask(__name__)
# secret key required for session support; replace with a secure random value in production
app.secret_key = 'replace-with-a-secure-random-secret'

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use session.get to avoid KeyError if 'username' not set
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
    if request.form.get('username') == 'admin' and request.form.get('password') == 'password':
        session['username'] = request.form.get('username')
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
    """
    Protected user settings page.
    Requires a logged-in session (session['username'] must be present).
    """
    username = session.get('username')
    if not username:
        # should not happen because decorator redirects, but defend here as well
        abort(403)
    return f'''
        <html>
            <head><title>Settings</title></head>
            <body>
                <h1>User Settings</h1>
                <p>Logged in as: {username}</p>
                <p><a href="/logout">Logout</a></p>
            </body>
        </html>
    '''

# optional logout route to clear the session
@app.route('/logout', methods=['GET'])
def logout():
    session.pop('username', None)
    return redirect(url_for('index'))