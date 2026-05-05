from functools import wraps
from flask import abort, g, Flask, request, redirect, session, url_for

app = Flask(__name__)
# Set a real secret key in production
app.secret_key = 'change-me-to-a-secure-random-value'

# ...existing code...
def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if session.get('username') is None:
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
        return 'Bad username or password'

# the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

"""user settings page"""
@app.route('/secret', methods=['GET'])
@ensure_logged_in
def settings():
    username = session.get('username')
    return f'''<html>
    <head><title>Settings</title></head>
    <body>
      <h1>Secret settings</h1>
      <p>Welcome, {username}.</p>
      <p>These are your secret settings. Change them with care.</p>
      <p><a href="{url_for('index')}">Home</a></p>
    </body>
    </html>'''