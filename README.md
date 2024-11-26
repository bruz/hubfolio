# Hubfolio

Builds a portfolio of GitHub repos you own or have contributed to, ranked by a combination of four metrics.

## Development

Prerequisites:

* [Leiningen](http://leiningen.org/)
* [Redis](http://redis.io/)
* A [GitHub](http://github.com/) account and OAuth token for it

Installing:

```bash
git clone git@github.com:bruz/hubfolio.git
cd hubfolio
cp .lein-env.example .lein-env
```

Modify the configuration in .lein-env. Get an OAuth token on your [GitHub Applications](https://github.com/settings/applications) page, under Personal access tokens.

```bash
lein repl
```

In the REPL:

```clojure
(user/reset)
```

## Deployment

To deploy to Heroku:

* Add a Redis addon, and set the REDIS_URL config (`heroku config:Set`) to the URL for it
* Set the OAUTH_TOKEN config to
* Do a `git push heroku master`, assuming `heroku` is your remote.
