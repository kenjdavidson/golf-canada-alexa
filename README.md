# Golf Canada Alexa Skill (Unofficial)

Provides basic Golf Canada membership functionality through an Amazon Alexa skill.

> This is an entirely unofficial application and may get shut down at any point due to the 
> nature in which it's interacting with the Golf Canada environment.  For information (or if
> you're Golf Canada and would like to hire me) please email me at 
> **ken.j.davidson@live.ca**

## Available Interactions

The following interactions are currently available:

## Future Interactions

The following interactions are under development or review.   More advanced requests may be available; eventually this list will be removed from the README and contained
within the project issues.

- [ ] Logging in with your Golf Canada account
- [ ] Terms of Use and Privacy Policy

> Note that this application is hosted on AWS and gains access to your Golf Canada
> account through Account Linking, which you can read about here:
> https://developer.amazon.com/en-US/docs/alexa/account-linking/add-account-linking.html
> No information is stored outside the Authentication information required by
> Alexa in order to facilitate account linking.

- [ ] Getting information regarding your handicap
- [ ] Getting information regarding your recent/yearly rounds
- [ ] Getting information about your friends/favorites handicap
- [ ] Getting information about your friends/favorites recent/yearly rounds
- [ ] Adding/saving new rounds

## Contributions

Please feel free to contribute with suggestions, issues founds, pull requests or discussion 
updates.   You should be able to tell from the current code what the expected styles and 
practices are... please continue with them.

### Issues and Suggestions

Open an Issue on the projects Github page.

### Pull Requests

If you've worked on a feature of a fix, please open a well documented pull request.

### Discussions

At this point I'm unsure whether the Wiki or Discussions will be available for this project, but if they
are I'll always welcome help with documentation.

## Golf Canada SSL Certificate

The one thing that's pretty effing annoying about Java is the lack of being up to date with SSL Certificate
roots.  In this instance, the https://golfcanada.ca certificate rooted by Go Daddy doesn't exactly match the 
Go Daddy root available in the Coretto JDK.   I haven't had a chance to test different JDKs but I'm assuming
it's the same across the board.

This may mean it will never work, or it means I just have to release the Authentication Lambda in another
language that works better, I'll have to test Javascript and/or Python.   I'll have to look into how annoying
this is to get working on Lambda:
- https://stackoverflow.com/questions/73399963/using-ssl-certificates-within-lambda
- https://aws.amazon.com/blogs/compute/implementing-mutual-tls-for-java-based-aws-lambda-functions/

Regardless, to get the tests and everything running locally:

```shell 
$JAVA_HOME\bin\keytool -importcert -file $PROJECT_ROOT\src\main\resources\client\_.golfcanada.ca.crt -keystore $JAVA_HOME\lib\security\cacerts -alias "GolfCanadaCert" -noprompt
```