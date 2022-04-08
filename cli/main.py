import typer
from commands.swarm import app as swarm
from commands.i2c import app as i2c
from commands.ui import load as ui_load

app = typer.Typer()
app.add_typer(swarm, name="swarm")
app.add_typer(i2c, name="i2c")

@app.callback(invoke_without_command=True)
def default(ctx: typer.Context) -> None:
    if ctx.invoked_subcommand is not None:
        return
    ui_load()

if __name__ == "__main__":
    app()
