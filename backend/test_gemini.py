import urllib.request
import json
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request(
    'https://generativelanguage.googleapis.com/v1beta/openai/chat/completions?key=fake_key',
    data=json.dumps({
        'model': 'gemini-1.5-flash',
        'messages': [{'role': 'user', 'content': 'hi'}]
    }).encode(),
    headers={'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(req, context=ctx) as r:
        print(r.read())
except Exception as e:
    print(e.read())
