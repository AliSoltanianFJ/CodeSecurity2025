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


@app.route('/settings')
@ensure_logged_in
def settings():
    return '''
        <html>
            <head>
                <title>Settings</title>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 800px; margin: 0 auto; padding: 20px; }
                    h1 { color: #333; }
                    .settings-content { background-color: #f5f5f5; padding: 20px; border-radius: 8px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Welcome to Settings, ''' + session['username'] + '''!</h1>
                    <div class="settings-content">
                        <p>Here you can manage your account settings.</p>
                        <a href="/change_password">Change Password</a><br>
                        <form action="/logout" method="post">
                            <button type="submit">Logout</button>
                        </form>
                    </div>
                </div>
            </body>
        </html>
    '''