from functools import wraps
from flask import abort, g, Flask, request, redirect, session

app = Flask(__name__)
app.secret_key = 'change-me'  # replace with a secure random value in production

# ...existing code...
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
# ...existing code...
@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    # Protected settings page — only accessible to authenticated users.
    username = session.get('username', 'user')
    return '''<html>
    <h1>User Settings</h1>
    <p>Welcome, {username}!</p>
    <ul>
      <li><a href="/profile">Edit profile</a></li>
      <li><a href="/change-password">Change password</a></li>
      <li><a href="/logout">Logout</a></li>
    </ul>
    </html>'''.format(username=username)