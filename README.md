# AdbLogger

Collect ADB logcat logs and send them over email

## Components

- LogCollector singleton class;
- xml/provider_path.xml & manifest's FileProvider in order to share internal files as Uri.
- MainApplication class for LogCollector one-time inti.
- MainActivity send logs over logic implementation.
