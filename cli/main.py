import typer
from commands.swarm import app as swarm
from commands.i2c import app as i2c
from commands.registration import app as registration
from commands.classifier import app as classifier
from commands.software import app as software

app = typer.Typer()
app.add_typer(swarm, name="swarm")
app.add_typer(i2c, name="i2c")
app.add_typer(registration, name="registration")
app.add_typer(classifier, name="classifier")
app.add_typer(software, name="software")

@app.callback(invoke_without_command=True)
def default(ctx: typer.Context) -> None:
    if ctx.invoked_subcommand is not None:
        return
    ui_load()

if __name__ == "__main__":
    app()
