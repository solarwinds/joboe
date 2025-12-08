# AGENTS.md: Solarwinds apm-joboe Development Guide

## Project Overview
See [README.md](README.md) for more details.

## Style
- Follow existing codebase conventions and style.

## Dev tips
- Run `mvn spotless:apply` to format files after making a change.
- Use comments sparingly and reserve them for obtuse code only.

## Testing instructions
- Write unit tests for any non-trivial change you make.
- Unit tests should test for correctness not structure.
- Add or update tests for the code you change, even if nobody asked.

## PR instructions
- Always run `mvn spotless:apply install` before committing.
- Use PR title that summarizes the change and write great PR descriptions.