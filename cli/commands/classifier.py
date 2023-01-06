from commands.common.classifier_command import *
import typer
from .adb import Device

app = typer.Typer()

@app.command()
def get():
    device = Device()
    classifiers = getClassifiers(device)
    typer.echo(classifiers)

@app.command()
def remove():
    device = Device()
    classifiers = removeClassifiers(device)
    typer.echo(classifiers)

@app.command()
def install(classifier: str):
    device = Device()
    classifiers = downloadClassifier(device, classifier)
    typer.echo(classifiers)
