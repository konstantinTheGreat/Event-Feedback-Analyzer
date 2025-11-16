# Event-Feedback-Analyzer
IBM Internship Exercise (setup instructions)

1. Clone the repo to your local repo
2. Ensure that you have access to the api ( like an api key )
3. Open docker on your device
4. In console, in ide, write the following command:
"docker build -t event-feedback-analyzer ."
6. Now, using the env file, write the following command to run the app:
"docker run -p 8080:8080 --env-file .env event-feedback-analyzer"
7. Go to the following link -> http://localhost:8080/swagger-ui/index.html#/event-controller
8. You will have a swagger interface opened, from which you can test and check different endpoints
