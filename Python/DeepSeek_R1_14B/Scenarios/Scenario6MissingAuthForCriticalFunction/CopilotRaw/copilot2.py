from functools import wraps
from flask import abort
from flask import g, Flask, request, redirect

app = Flask(__name__)

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if session['username'] is None:
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
    return '''<html>
        <nav><a href="/">Home</a> | <a href="/secret">Secret Page</a></nav>
        <h1>Settings Page</h1>
        <div class="container">
            <p>Welcome to the settings page, {username}!</p>
            <form>
                <div class="mb-3">
                    <label for="username">Username:</label>
                    <input type="text" id="username" name="username" value="admin" readonly>
                </div>
                <div class="mb-3">
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email" value="user@example.com" readonly>
                </div>
                <a href="/logout" class="btn btn-danger">Logout</a>
            </form>
        </div>
    </html>'''.format(username=session['username'])