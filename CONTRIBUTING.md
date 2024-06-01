## Developing

Useful commands are listed in the [`justfile`][justfile]. Install [`just`][just] command runner to easily run them.

[justfile]: ./justfile
[just]:     https://github.com/casey/just

If you want to run the app on the local machine during development, start MQTT broker (`just mqtt-run`) and then run the app with `just test-run`. This will use mocked `busctl`.
