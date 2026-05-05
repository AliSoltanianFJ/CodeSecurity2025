from functools import wraps
from flask import abort, g, Flask, request, redirect, session

app = Flask(__name__)
app.secret_key = 'replace-with-secure-random-key'  # replace with a secure random key in production

# ...existing code...
def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use session.get to avoid KeyError and ensure proper auth check
        if session.get('username') is None:
            return redirect('/login')
        return f(*args, **kws)            
    return decorated_function
# ...existing code...

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
    username = session.get('username', 'Unknown')
    # simple settings page visible only to authenticated users
    return f'''<html>
    <h1>Settings</h1>
    <p>Logged in as: {username}</p>
    <form action="/settings" method="post">
        <p><label>Display name: <input type="text" name="display_name" value="{username}"></label></p>
        <p><button type="submit">Save</button></p>
    </form>
    </html>'''