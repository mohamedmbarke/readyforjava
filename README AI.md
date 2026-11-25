# readyforjava

A terminal-based Java learning platform. Each topic pairs a concise explanation with curated
external tutorials and hands-on practice exercises, in the interactive spirit of CodeStepByStep.
The goal: help students reach **at least 70% foundational readiness** in Java.

## Features
- 12 core topics: basics, variables, operators, conditionals, loops, strings, arrays, methods,
  classes, inheritance & interfaces, exceptions, collections
- Syntax-highlighted code samples
- Curated links to Oracle Tutorials, W3Schools, dev.java and CodeStepByStep
- Auto-checked exercises with hints
- Progress dashboard with a **readiness percentage** measured against the 70% goal
- Customizable colors: black background by default; change background, text and heading colors
  by name or `#RRGGBB` hex. Settings persist in `~/.readyforjava/`

## Requirements
JDK 17+, Maven 3.8+, and a terminal with ANSI/truecolor support (Windows Terminal, macOS Terminal/iTerm2, most Linux terminals).

## Build and run
```bash
mvn clean package
java -jar target/readyforjava.jar
```

## Project layout
```
src/main/java/com/readyforjava/
  Main.java                  entry point
  App.java                   menus and screens
  ui/Terminal.java           themed console I/O
  ui/Theme.java              colors + persistence
  ui/Highlighter.java        Java syntax coloring
  model/                     Topic, Exercise, Link, Curriculum records
  content/CurriculumLoader   parses curriculum.txt
  progress/ProgressTracker   saves completed exercises, computes readiness
src/main/resources/curriculum.txt   all lessons, links and exercises
```

## Adding content
Edit `src/main/resources/curriculum.txt`. Fields are separated by ` @@ `:
```
@TOPIC id @@ Title
@EXPLAIN
free text; lines indented 4 spaces are shown as highlighted code
@LINK Title @@ https://...
@EX short  @@ prompt (use \n for new lines) @@ (blank) @@ answer1;;answer2 @@ hint
@EX choice @@ prompt @@ A) one;;B) two;;C) three @@ b @@ hint
```

## Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit: readyforjava"
git branch -M main
git remote add origin https://github.com/<you>/readyforjava.git
git push -u origin main
```
