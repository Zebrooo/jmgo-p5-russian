# JMGO P5 battery percentage in FLauncher

## Goal

Show the projector's real remaining battery percentage in the existing top status panel without changing its approved visual layout.

## Architecture

The native Android bridge reads `/sys/class/power_supply/mcu_slave_battery/capacity` and `status`, because JMGO's standard battery broadcast reports `present=false` and an unknown status. A pure selector validates the MCU values and falls back to `ACTION_BATTERY_CHANGED` when they are unavailable. Flutter receives `batteryPercent` and the existing `pluggedIn` boolean and renders `52% · Батарея` or `52% · Зарядка`.

## Failure handling

Capacity is accepted only in `0..100`. MCU status `Charging` or `Full` means charging; a missing MCU status falls back to `EXTRA_PLUGGED`. If neither capacity source is valid, the panel keeps the existing label without a percentage.

## Verification

- Unit-test MCU precedence, broadcast fallback, invalid input, and charging semantics.
- Widget-test the Russian battery and charging labels.
- Build and install the launcher without clearing its database.
- Compare the displayed percentage to the MCU sysfs value and confirm Wi-Fi/settings controls remain interactive.

