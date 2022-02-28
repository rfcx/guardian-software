# Guardian CLI Diagnostic Toolkit

--

## Getting started

Connect to a Guardian over wifi and then run one of these commands:
- `swarm id` Get the Swarm device identifier

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
