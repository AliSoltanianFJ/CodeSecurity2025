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

from flask import session, escape, request, redirect, render_template

@app.route('/settings', methods=['GET', 'POST'])
@ensure_logged_in
def settings():
    if request.method == 'POST':
        new_username = request.form.get('new_username')
    if not new_username:
        return render_template('settings.html', error='Username cannot be empty.'), 400
    if len(new_username) > 150:
        return render_template('settings.html', error='Username is too long.'), 400
        session['username'] = new_username
        return redirect('/secret')
    else:
        username = escape(session['username'])
        return render_template('settings.html', username=username)
