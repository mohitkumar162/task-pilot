# How JWT Works In This Project (Beginner Explanation)

## The idea in one line
Instead of sending your email+password on EVERY request (like Basic Auth did),
you send them ONCE to get a "token" (a signed string). After that, you attach
that token to every request, and the server trusts it until it expires.

## The flow

1. User registers:
   POST /api/auth/register
   { "name": "Mohit", "email": "mohit@test.com", "password": "1234", "role": "DEVELOPER" }

2. User logs in:
   POST /api/auth/login
   { "email": "mohit@test.com", "password": "1234" }

   Server checks the password, then returns:
   { "token": "eyJhbGciOiJIUzI1NiJ9....", "role": "DEVELOPER", "name": "Mohit" }

3. For every other request, attach the token as a header:
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9....

4. JwtAuthFilter.java runs on every request:
   - Reads the "Authorization" header
   - Checks the token is valid and not expired (JwtUtil.java does this)
   - Pulls the email + role out of the token
   - Tells Spring Security "this user is authenticated with this role"

5. SecurityConfig.java then decides what each role is ALLOWED to do
   (e.g. only MANAGER can create projects).

## Why this is better than Basic Auth
- Basic Auth sends your raw password on every single request (bad).
- JWT sends your password ONCE at login, then just a token afterward.
- The token itself contains who you are (email) and your role — the
  server doesn't need to hit the database to check "who is this user"
  on every request, it just reads the token.

## Key files to look at (in order of importance)
1. JwtUtil.java       -> creates and reads tokens
2. JwtAuthFilter.java -> checks the token on every incoming request
3. SecurityConfig.java -> decides who can access what
4. AuthController.java -> where tokens get created (login endpoint)

## Testing in Postman
1. Call POST /api/auth/register to create a user
2. Call POST /api/auth/login -> copy the "token" value from the response
3. For any other request, go to the "Authorization" tab in Postman,
   choose "Bearer Token", and paste the token in
