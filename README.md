# J% (j-mod)

J% is a small Java language extension for embedding DSLs as first-class `external` types. This repository is a **source-to-source prototype compiler**: `.jmod` files become Java classes, then `javac` produces bytecode.

The host syntax, package layout (`org.jmod.*`), and module API follow the J% thesis. Regex, SQL, GetSet, and JSON are implemented as pluggable compiler modules.

## Requirements

- JDK 17 or later (`JAVA_HOME` on the PATH)
- no other tools: the tree includes a Maven wrapper

## Build

```sh
./mvnw test
./mvnw package
```

The shaded compiler jar is `target/jmod-0.1.0-SNAPSHOT.jar`.

## Compile

```sh
java -jar target/jmod-0.1.0-SNAPSHOT.jar -i examples/simpleregex -o out
java -jar target/jmod-0.1.0-SNAPSHOT.jar --help
```

GNU-style options; short and long forms are equivalent (`-i DIR` / `--input-dir=DIR`). Operands are extra input files or directories. Default output directory is `work/`.

| Option | Meaning |
| --- | --- |
| `-i`, `--input-dir=DIR` | Input trees (repeatable) |
| `-o`, `--output-dir=DIR` | Generated `.java` / `.class` output |
| `-n`, `--no-javac` | Generate Java only |
| `-j`, `--jmod-only` | Skip copied `.java` sources |
| `-l`, `--module-list` | List DSL modules |
| `-m`, `--metrics=FILE` | Write a JSON metrics report |
| `-h`, `--help` | Help |

`*.jmod` files are compiled as externals. Sibling `.java` files (configuration classes, `main`) are copied into the output tree and compiled with javac unless `-j` / `-n` say otherwise.

## Language

An external type is a Java-looking declaration whose body is DSL text, not Java statements. Host values are spliced with `#[name]<JavaType>` (the type defaults to `String`).

```java
package examples.simpleregex;

import org.jmod.dsl.regex.Regex;
import org.jmod.dsl.regex.RegexConfiguration;

public external IpAddress extends Regex<RegexConfiguration> {
([0-9]{1,3}\\.){3}[0-9]{1,3}
}
```

After compilation that is an ordinary Java class:

```java
IpAddress ip = new IpAddress();
ip.matches("127.0.0.1");
```

The configuration type argument may be omitted; the module default is used (`RegexConfiguration`, `SQLConfiguration`, `GetSetConfiguration`, `JsonConfiguration`).

## Modules

### Regex

Body is a `java.util.regex` pattern, checked with `Pattern.compile` at compile time. Runtime API: `matches`, `find`, `replace` / `replaceAll`, `group`.

Engine is JDK only (`REGEX_ENGINE=jdk`).

### SQL

Body is SQL. Placeholders become JDBC parameters:

```java
public external SelectExample extends SQLQuery<SimpleConf> {
select * from sqlexample where sqle_primary = #[prim]<int>
}
```

`new SelectExample(7).getStatement(connection)` prepares `… = ?` and `setInt`.

Java arrays are SQL `IN` lists (JDBC has no portable `IN ?` binding). `byte[]` stays a BLOB.

```sql
select * from users where nickname in #[names]<String[]>
select * from t where id in #[ids]<int[]>
```

Empty or null arrays throw `SQLException` rather than emitting `IN ()`.

Subclass `SQLConfiguration` to enable compile-time checks:

| Field | Role |
| --- | --- |
| `SQLMOD_NS_AWARE` / `SQLMOD_NS_URI` | Load `CREATE TABLE` DDL; reject unknown tables/columns and Java/SQL type mismatches |
| `SQLMOD_LIVE_TEST` + JDBC url/driver/login | Run the query with default literals against a live database |

Relative `file://./schema.sql` URIs resolve against the configuration source directory. Checks are vendor-neutral (not MySQL-only).

### GetSet

The body is a list of fields. The module generates a Java class with fields and optional getters/setters.

```java
public external Person extends GetSetType<GetSetConfiguration> {
#[name]<String> #[age]<int>
}
```

`GS_GEN_GETTER` / `GS_GEN_SETTER` (default true).

### JSON

The body is a JSON value with `#[name]<JavaType>` at value positions (not inside JSON strings):

```java
public external Person extends JsonObject<JsonConf> {
{
  "name": #[name]<String>,
  "age": #[age]<int>,
  "tags": #[tags]<String[]>
}
}
```

`new Person("Ada", 36, tags).toJson()` builds a JSON document with values encoded by Jackson. `toJsonNode()` returns a parsed tree.

Subclass `JsonConfiguration` to validate against a JSON Schema at compile time (dummy literals) and again at `toJson()` (real values):

| Field | Role |
| --- | --- |
| `JSONMOD_SCHEMA_AWARE` / `JSONMOD_SCHEMA_URI` | Load a JSON Schema document; reject instances that do not conform (including Java/JSON type mismatches) |

Drafts 4, 6, 7, 2019-09, and 2020-12 are accepted (`$schema` selects the dialect). Relative `file://./person.schema.json` URIs resolve against the configuration source directory.

## Examples

```sh
java -jar target/jmod-0.1.0-SNAPSHOT.jar -i examples/simpleregex -o out
java -cp out:target/jmod-0.1.0-SNAPSHOT.jar examples.simpleregex.Main
```

- `examples/simpleregex` — IP address regex
- `examples/simplesql` — parameterized `SELECT` plus `SimpleConf` with schema checking (`examples/simplesql/schema.sql`)
- `examples/simplejson` — JSON object plus `JsonConf` with JSON Schema checking (`examples/simplejson/person.schema.json`)

## Tests

`./mvnw test` runs unit tests and compiles the vendored historical `.jmod` ports under `src/test/resources/jmod-ports/` (a subset of [jmod-ports](https://github.com/bkarak/jmod-ports)).

## References

- Vassilios Karakoidas, *J%*, PhD thesis, Athens University of Economics and Business.
- V. Karakoidas, D. Mitropoulos, P. Louridas, D. Spinellis, [A type-safe embedding of SQL into Java using the extensible compiler framework J%](https://www.spinellis.gr/pubs/jrnl/2015-JLSS-jmod-sql/html/journal.pdf), *Computer Languages, Systems & Structures*, 2015.

## License

[MIT](LICENSE).
