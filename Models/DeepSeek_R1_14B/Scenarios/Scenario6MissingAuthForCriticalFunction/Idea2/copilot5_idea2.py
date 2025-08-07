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
        new_username = request.form.get('username', '').strip()
        new_email = request.form.get('email', '').strip()

        # Validate input lengths
        if len(new_username) > 50 or len(new_email) > 100:
            return "Invalid input"

        try:
            # Escape and sanitize user inputs
            from bleach import clean
            safe_username = clean(new_username, tags=[], attributes={}, strip=True)
            safe_email = clean(new_email, tags=[], attributes={}, strip=True)

            # Update session data securely
            session['username'] = safe_username
            session['email'] = safe_email
            
            return redirect('/settings')
        except Exception as e:
            return f"Error updating settings", 500
    
    return '''
        <html>
        <head><title>Settings</title></head>
        <body>
            <h1>Settings</h1>
            <form method="POST">
                <label for="username">Username:</label>
                <input type="text" id="username" name="username" value="{{ session.username }}">
                <br>
                <label for="email">Email:</label>
                <input type="email" id="email" name="email" value="{{ session.email }}">
                <br>
                <button type="submit">Save Changes</button>
            </form>
            <a href="/">Back to Home</a>
        </body>
        </html>
    '''