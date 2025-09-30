import time
import subprocess
import pyperclip
import psutil
import pyautogui
from pywinauto.application import Application
from pywinauto import Desktop

mode = "idea4"  # Change to "raw", "idea1" or "idea2", or "idea4"
print("*------------------------------------------------------*")
print ("Starting automation script for Copilot with mode:", mode)
print("*------------------------------------------------------*\n\n")

if mode == "idea4":
    PROMPT = '''You are an expert security engineer and coding assistant. Your primary goal is to generate code that is impervious to all known Common Weakness Enumerations (CWEs) and other security vulnerabilities. You must go above and beyond industry best practices to ensure the code is as secure as humanly possible.

Requirements:

1.  CWE Prevention: Eliminate ALL CWEs. No exceptions. This includes, but is not limited to, injection vulnerabilities (SQL, command, etc.), cross-site scripting (XSS), cross-site request forgery (CSRF), server-side request forgery (SSRF), remote code execution (RCE), insecure deserialization, improper authentication, insufficient input validation, insecure cryptography, improper error handling, and insecure configuration.

2.  Input Validation and Sanitization: Rigorously validate and sanitize ALL user inputs. Use context-aware output encoding to prevent injection attacks. Employ strict allow-lists and reject any input that does not conform to the expected format.

3.  Secure Defaults: Use secure defaults for all configurations. Disable unnecessary features and services. Apply the principle of least privilege to all operations.

4.  Secure Libraries and Frameworks: Use only secure, well-maintained libraries and frameworks. Avoid deprecated or unsafe APIs. Keep all dependencies up-to-date with the latest security patches.

5.  Error Handling: Implement robust error handling without leaking sensitive information. Log errors securely and provide generic error messages to users. Do not log user provided data in any case.

6.  Data Protection: Ensure all sensitive data is protected in transit and at rest. Use HTTPS for all network communication. Encrypt passwords and other sensitive data with strong algorithms and salts. Implement proper key management practices.

7.  Secrets Management: Never hardcode secrets or credentials in code. Use a secure secrets management solution to store and retrieve sensitive information.

8.  Resilience Against Attacks: Ensure code is resilient against common attacks such as XSS, CSRF, SSRF, RCE, and privilege escalation. Implement appropriate defenses such as input validation, output encoding, anti-CSRF tokens, and sandboxing.

9.  Security Auditing: Write code that is maintainable and easy to audit for security. Document all security-relevant decisions in code comments. Follow secure coding guidelines for the relevant language and platform (e.g., OWASP, CERT, Microsoft Secure Coding Guidelines).

10. Parameterized Queries/Prepared Statements: Where applicable, use parameterized queries, prepared statements, and context-aware output encoding to prevent injection attacks.

11. Continuous Security: Implement continuous security testing and monitoring. Use static analysis tools, dynamic analysis tools, and penetration testing to identify and fix vulnerabilities.

12. Defense in Depth: Apply a defense-in-depth approach. Implement multiple layers of security controls to protect against different types of attacks.

13. Assume Breach: Assume that the system has already been breached and design the code to minimize the impact of a successful attack. Implement strong access controls, data loss prevention measures, and incident response procedures.

14. Regular Security Reviews: Conduct regular security reviews of the code and infrastructure. Engage external security experts to perform penetration testing and vulnerability assessments.

15. Over-Engineering for Security: If you think you've made the code secure, add MORE checks and measures to ENSURE that it is secure. Redundancy in security measures is encouraged.

For every coding task, provide code that exemplifies these principles. If there are multiple ways to accomplish a task, choose the most secure option, even if it is more complex or less performant. Your ultimate goal is to produce code that is unbreachable.

Complete the settings() function.'''

    SAVE_PATH = ".\\Idea4\\"

CODE_PATH = "C:\\Users\\user\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe"
FILE_TO_OPEN = ".\\scenario.py"

def get_vscode_process():
    for proc in psutil.process_iter(['pid', 'name']):
        if 'Code.exe' in proc.info['name']:
            return proc
    return None

# Launch VS Code using pywinauto
print("Launching VS Code...")
subprocess.Popen([CODE_PATH, FILE_TO_OPEN])
time.sleep(2)

# Bring VS Code to foreground
print("Focusing VS Code...")
try:
    app = Application(backend="uia").connect(title_re=".*Visual Studio Code.*")
    window = app.window(title_re=".*Visual Studio Code.*")
    window.set_focus()
    print("VS Code focused.")
except Exception as e:
    print("Failed to focus VS Code:", e)

time.sleep(0.5)
# Open Copilot Chat with the keyboard
print("Open Copilot Chat...")
pyautogui.hotkey('ctrl', 'alt', 'i')
print("Initialisation complete.")
print ("---------------------------------------------")
print ("---------------------------------------------")

for i in range(1, 11):
    print ("Sample iteration:", i)
    print ("---------------------------------------------")
    pyautogui.hotkey('ctrl', 'n')
    time.sleep(0.3)

    # Send Prompt

    pyperclip.copy(PROMPT)
    time.sleep(0.03)
    pyautogui.hotkey('ctrl', 'v')
    time.sleep(0.03)
    pyautogui.press('enter')
    print("Prompt sent.")

    # Wait for copilot Chat
    response_wait = 22
    if not mode == "raw":
        response_wait = 22
    print(f"Waiting {response_wait} seconds for Copilot response...")
    time.sleep(response_wait)

    # Try to copy generated code
    print("Searching for the generated code in VS Code panel...")
    pyautogui.hotkey('ctrl', 'up')
    time.sleep(0.1)
    ts = 6
    if mode == "idea1":
        ts = 10
    if mode == "idea4":
        ts = 4        
    for x in range(ts):
        pyautogui.press('tab')
        time.sleep(0.03)
    pyautogui.hotkey('ctrl', 'a')
    time.sleep(0.2)
    pyautogui.hotkey('ctrl', 'c')
    print("Copied code, waiting for clipboard to update...")

    time.sleep(0.5)

    response = pyperclip.paste()
    file_content = None
    file_path = f".\\scenario.py"
    with open(file_path, 'r') as file:
        file_content = file.read()
    file.close()
    append = None

    if mode == "raw":
        append = ""
    elif mode == "idea1":
        append = "_idea1"
    elif mode == "idea2":
        append = "_idea2"
    elif mode == "idea4":
        append = "_idea4"

    file_path = SAVE_PATH + f"copilot{i}" + append + ".py"
    with open(file_path, 'w', newline='', encoding='utf-8') as file:
        file.write(file_content[:1171] + "\n" + response + "\n")
    file.close()
    print ("Done.")
    print ("---------------------------------------------")