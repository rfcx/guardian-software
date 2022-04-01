# Guardian CLI Diagnostic Toolkit

--

## Getting started

Connect to a Guardian over wifi and then run one of these commands:
- `swarm status` Get the Swarm status (0011010-1 is ON / 0000010-1 is OFF)
- `swarm id` Get the Swarm device identifier
- `swarm gps` Get the Swarm GPS Fix Quality (Example return $GS 109,214,9,0,G3*46) 
- `swarm fw` Get the Swarm firmware version of swarm (Example return 2021-07-16-00:10:21,v1.1.0*74)
- `swarm dt` Get the Swarm Datetime of swarm (Example return 20190408195123,V*41)
- `i2c status` Get the i2c status (0x0001 is ON / 0x0000 is OFF)
- `i2c input-info` Get the input information (Voltage, Currect)
- `i2c system-info` Get the  system information (Voltage)
- `i2c bat-info` Get the battery information (Voltage, Current, battery percentage)

- `ui` Show ui diplay i2c and swarm information.
- `pyinstaller --onefile --noconsole make_ui.py` create program make_ui.exe for monitor i2c and swarm information. path to program : dist/make_ui.exe

There are 2 ways of running commands (with or without Docker).

## Running commands using Docker (recommended)

```
docker build -t guardian-cli .
docker run --rm guardian-cli swarm id
```

The first command (`docker build`) compiles the code into a docker image. It only needs to be run once.

The second command (`docker run`) runs a command on the cli (e.g. `swarm id`). The `--rm` removes the container after the command executes.

For development, link your local source folder to the container's source folder (using `-v HOST_FOLDER:CONTAINER_FOLDER`):

```
docker run --rm -v ${PWD}:/app guardian-cli swarm id
```

## Running commands without Docker (regular Python)

You need Python 3.7+.

Install the dependencies:

```
pip -r requirements.txt
```

Run:

```
python main.py swarm id
```
