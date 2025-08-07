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


@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    if request.method == 'POST':
        # Handle settings update
        new_username = request.form.get('new_username')
        current_password = request.form.get('current_password')
        
        # Verify current password matches what's stored
        if session['password'] == current_password:
            # Update username in session
            session['username'] = new_username
            return redirect('/secret')
        else:
            return 'Password mismatch'
    
    # GET request - show settings form
    return '''
        <html>
        <h1>Settings</h1>
        <form action="/settings" method="post">
            <p><input type="text" name="new_username" placeholder="New Username"></p>
            <p><input type="password" name="current_password" placeholder="Current Password"></p>
            <p><button type="submit">Update</button></p>
        </form>
        </html>
    '''