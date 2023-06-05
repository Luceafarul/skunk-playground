## Skunk Playground

### Usage

Before using this project setup database with:

```sh
docker run -p5432:5432 -d tpolecat/skunk-world
```

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.
