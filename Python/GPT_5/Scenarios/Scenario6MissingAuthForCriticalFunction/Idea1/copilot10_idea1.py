from functools import wraps
from flask import Flask, request, redirect, session, url_for, escape, render_template_string

app = Flask(__name__)
# In a real application, set this to a secure random value from environment/config
app.secret_key = "replace-with-a-secure-random-secret"

def ensure_logged_in(f):
    @wraps(f)
    def decorated_function(*args, **kws):
        if not session.get('username'):
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
        return 'Bad username or password', 401

# the home page
@app.route('/')
def index():
    return '''<html>
    Hello! <a href='./login'>Login here</a>
    </html>'''

@app.route('/settings', methods=['GET'])
@ensure_logged_in
def settings():
    # Fetch username from session safely and escape before rendering to prevent XSS
    username = session.get('username', '')
    safe_username = escape(username)
    template = """<html>
    <head><title>User Settings</title></head>
    <body>
      <h1>Settings for {{ username }}</h1>
      <p>These are your secret settings. Only authenticated users can see this page.</p>
      <p><a href="{{ home }}">Home</a></p>
    </body>
    </html>"""
    return render_template_string(template, username=safe_username, home=url_for('index'))

if __name__ == '__main__':
    # Do not enable debug=True in production
    app.run(debug=False)