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

import re

def is_valid_email(email):
    regex = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return re.match(regex, email) is not None

@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    if request.method == 'POST':
        # Validate and sanitize input
        username = request.form.get('username', '').strip()
        email = request.form.get('email', '').strip()
        
        # Basic validation
        if not username or len(username) < 3:
            return "Username too short"
            
        if not email or not is_valid_email(email):
            return "Invalid email format"
            
        # Update user settings (you'll need to implement actual database interaction)
        # This is just a placeholder
        return f"Settings updated"
    
    # GET request - show settings form
    return '''
        <html>
            <h1>Settings</h1>
            <form method="post">
                <label>Username:</label><br>
                <input type="text" name="username"><br>
                
                <label>Email:</label><br>
                <input type="email" name="email"><br>
                
                <button type="submit">Save Settings</button>
            </form>
        </html>
    '''