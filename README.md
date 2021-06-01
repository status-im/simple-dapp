# Desription

Status Simple Dapp is a helper Dapp for developers and testers of Status App.

### Installation:

```
yarn install
yarn run build
```

## Development Mode

### Run application:

```
lein figwheel-repl
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Mode

### Build CLJS:

```
yarn run compile
```

Open `resources/public/index.html` in the browser.

### Deploy

```
yarn run deploy
```

## Continuous Integration

Status Jenkins instance builds the `master` branch with this job:

https://ci.status.im/job/website/job/simpledapp.status.im/

Which deploys it to `gh-pages` branch which is published at:

* https://simpledapp.status.im/
