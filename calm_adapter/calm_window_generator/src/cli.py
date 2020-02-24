#!/bin/env python3

from datetime import date

import click
from tqdm import tqdm

from window_generator import WindowGenerator as _WindowGenerator


class WindowGenerator(_WindowGenerator):

    topic_arn = "arn:aws:sns:eu-west-1:760097843905:calm-windows"

    def __init__(self, start, end=None):
        super().__init__(self.topic_arn, start, end=end)

    @property
    def windows(self):
        return tqdm(list(super().windows))


@click.group()
def cli():
    pass


@cli.command()
def full_ingest():
    start = date(2015, 9, 30)  # First date returning any records
    end = date.today()
    WindowGenerator(start, end).run()


@cli.command()
def today():
    start = date.today()
    WindowGenerator(start).run()


@cli.command()
@click.argument("start", type=click.DateTime(["%Y-%m-%d"]))
@click.argument("end", type=click.DateTime(["%Y-%m-%d"]))
def date_range(start, end):
    WindowGenerator(start.date(), end.date()).run()


if __name__ == "__main__":
    cli()
