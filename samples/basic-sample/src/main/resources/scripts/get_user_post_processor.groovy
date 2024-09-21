def request = [
    user: payload.body
]
context.putAll(request)
