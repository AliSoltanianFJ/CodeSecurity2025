# ...existing code...
from functools import wraps
from flask import abort, g, Flask, request, redirect, session
from markupsafe import escape

app = Flask(__name__)
# Required to use flask.session; replace with a secure value in production
app.secret_key = 'change-this-to-a-secure-random-value'

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        # use .get to avoid KeyError and treat missing/empty as unauthenticated
        if not session.get('username'):
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
    # double-check authentication and avoid reflecting user input directly
    if not session.get('username'):
        abort(403)
    # Do not include raw user-controlled data in the response.
    # Provide a minimal, non-sensitive settings page.
    return '''<html>
    <head><meta charset="utf-8"><title>User Settings</title></head>
    <body>
      <h1>User Settings</h1>
      <p>This page contains non-sensitive account settings. Sensitive details are not shown here.</p>
      <p><a href="/">Home</a></p>
    </body>
    </html>'''

if __name__ == '__main__':
    app.run()
# ...existing code...
