# RPi MQTT Monitor for OpenMower and Home Assistant

This repository contains a service that monitors Raspberry Pi device and sends various metrics to the MQTT broker. It is designed to be run on an [OpenMower] mower and metrics are auto discovered by Home Assistant.

> [!NOTE]
> This project is very much inspired by [rpi-mqtt-monitor] and my previous attempt of running it on the OpenMower [rpi-mqtt-monitor-docker].

[rpi-mqtt-monitor]:        https://github.com/hjelev/rpi-mqtt-monitor
[rpi-mqtt-monitor-docker]: https://github.com/2m/rpi-mqtt-monitor-docker
[OpenMower]:               https://openmower.de/

## Usage

### Running on the OpenMower

Copy the [`rpimon.service`][rpimon-service] file to `/etc/systemd/system/` on your OpenMower.

Then enable and start the service:

```bash
sudo systemctl enable rpimon.service
sudo systemctl start rpimon.service
```

This will start publishing configured sensor values to OpenMower MQTT broker:

![mqttui][]

You can also check the logs of the service to troubleshoot if needed:

```bash
sudo podman logs -f rpimon
```

[rpimon-service]: ./rpimon.service
[mqttui]:         ./docs/mqttui.png

### Configuring Home Assistant MQTT bridge

Home Assistant discovery messages are published together with sensor messages. So the only thing you need to do is to configure the MQTT bridge.

1. Go to [Mosquitto add-on][mosquitto-addon] configuration page:
    `Settings --> Addons --> Mosquitto broker --> Configuration`

1. Enable configuration customization by adding the following to the `Customize` field:

    ```yaml
    active: true
    folder: mosquitto
    ```

1. Add the following configuration to `/share/mosquitto/openmower.conf` file on your HA machine:

    ```
    connection openmower-to-homeassistant
    address openmower.local:1883
    bridge_protocol_version mqttv50
    topic # in 0 rpimon/ rpimon/
    topic # in 0 homeassistant/ homeassistant/
    ```

1. This will forward all monitoring and discovery messages from OpenMower to Home Assistant. Restart the MQTT add-on and you will see a new device under MQTT integration.

    ![device][]

[device]: ./docs/device.png

[mosquitto-addon]: https://github.com/home-assistant/addons/tree/master/mosquitto

## Configuration details

There are a couple of configuration options that can be set in the `rpimon.service` file as environment variables.

| Environment Variable        | Default value | Description                                     |
|-----------------------------|---------------|-------------------------------------------------|
| `RPIMON_TICK`               | `5 seconds`   | Interval between sensor values are sent to MQTT |
| `RPIMON_MQTT_HOST`          | `localhost`   | MQTT broker host                                |
| `RPIMON_MQTT_PORT`          | `1883`        | MQTT broker port                                |
| `RPIMON_TOPIC_PREFIX`       | `rpimon`      | MQTT topic prefix                               |
| `RPIMON_WIRELESS_DEVICE`    | `wlan0`       | Wireless device to monitor                      |
| `RPIMON_MAC_FRIENDLY_NAMES` | *empty*       | User friendly names for MAC addresses           |

### `RPIMON_TOPIC_PREFIX`

Controls the prefix used in the MQTT topics for the Home Assistant discovery and sensor messages. For example, with the default prefix value, the following topic names will be used for `wifi_bssid` sensor:

* discovery: `homeassistant/sensor/rpimon/openmower_wifi_bssid/config`
* sensor state: `rpimon/openmower/wifi_bssid`

### `RPIMON_MAC_FRIENDLY_NAMES`

This environment variable allows to map AP station MAC addresses to user friendly names. It needs to be a string where each entry is separated by a comma and each entry is MAC address and its friendly name separated by an equals sign. For example: `00:11:22:33:44:55=Kitchen,66:77:88:99:AA:BB=Living Room`

## Sensor details

Most of the sensors are self-explanatory. However a couple of sensors need some extra mention.

### `binary_sensor.openmower_wifi_bssid`

This sensor is used to monitor the WiFi BSSID of the OpenMower WiFi connection. It is especially useful when you have multiple access points with the same SSID and want to track which one your OpenMower is connected to.

This sensor value if always `ON`. Only the `friendly_name` changes with the value of the BSSID. This is designed such way for the [Home Assistant Prometheus][ha-prometheus] integration. It only exports certain sensor types and only if the sensor has numerical value. However, lucky to us, it also exports `friendly_name` as a label.

```
homeassistant_binary_sensor_state{
    domain="binary_sensor",
    entity="binary_sensor.openmower_wifi_bssid",
    friendly_name="openmower 3C:28:6D:6C:F9:00",
    instance="hassio.local:8123",
    job="home-assistant"
}
```

Here the `friendly_name` also is prefixed with `openmower` - HA device name. This is done automatically in HA and I was not able to find how to turn it off.

Now we can use Prometheus query `homeassistant_entity_available{entity="binary_sensor.openmower_wifi_bssid"}` and a `Rename fields by regex` transformation with **Match** argument `.*friendly_name="openmower (.*)", instance.*` and **Replace** argument `$1` to get the following graph:

![grafana-wifi][]

Here we also use `RPIMON_MAC_FRIENDLY_NAMES` to convert MAC addresses to more friendlier names.

[grafana-wifi]: ./docs/grafana-wifi.png

### `sensor.openmower_ap_strength_...`

These are sensors created for every BSSID that the OpenMower sees which is on the same SSID as the active WiFi connection. `entity_id` of these sensors end with a BSSID value with the `:` removed.

These are useful to track if your WiFi network AP placement is optimally used.

[ha-prometheus]: https://www.home-assistant.io/integrations/prometheus/
