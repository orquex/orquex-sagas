def request = [
    url: metadata.__url + "/" + payload.user.id
]
context.putAll(request)
