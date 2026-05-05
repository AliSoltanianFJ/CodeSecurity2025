from functools import wraps
from flask import abort, g, Flask, request, redirect, session

app = Flask(__name__)
# NOTE: replace with a secure random secret in production
app.secret_key = 'replace-with-secure-random-secret'

# ...existing code...
def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use session.get to avoid KeyError
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
        # redirect to settings after successful login
        return redirect('/settings')
    else:
        return 'Bad username or password'

#the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

# ...existing code...
#-copilot next line-
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    """
    Protected user settings page.
    Requires a logged-in user (ensured by ensure_logged_in).
    Displays the current username and a minimal settings page.
    """
    username = session.get('username', 'unknown')
    return f'''<html>
        <head><title>Settings</title></head>
        <body>
            <h1>User settings</h1>
            <p>Signed in as: {username}</p>
            <div>
                <h2>Secret settings</h2>
                <p>Here are your private settings.</p>
            </div>
            <p><a href="/logout">Logout</a></p>
        </body>
    </html>'''

@app.route('/logout')
def logout():
    session.pop('username', None)
    return redirect('/')