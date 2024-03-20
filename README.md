String Expanding Formatter
==========================

## Basics

The Expander is a simple class for expanding string templates with substitution parameters. It is inspired by Java's [MessageFormat](https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html) class and uses `{}` as the basic replacement token.

To expand a template:

```java
Expander.expand("{} followed by {}", "a", "b");
```

produces the string

```java
"a followed by b"
```

The first argument to `expand` is the _template_, followed by an arbitrary number of _parameters_. The parameters can also be referenced by positional number, starting from **1** (note this differs from Java's MessageFormat class which numbers from **0**):

```java
Expander.expand("{2} followed by {1}", "a", "b");
// produces "b followed by a"
```

Parameter numbers exceeding the number of available parameters are replaced by the empty string `""`.

```java
Expander.expand("{1}, {2}, and {3}!", "a", "b");
// produces "a, b, and !"
```

## Edits

The replacement token `{}` can contain not only a parameter number, but also a sequence of one or more _edits_ that expand into the template an edited version of the parameter:

* `trim` trims leading and trailing whitespace.
* `upper` or `toupper` converts to upper case.
* `lower` or `tolower` converts to lower case.
* `urldecode` URL decodes.
* `urlencode` URL encodes.
* `base64decode` or `b64decode` base-64 decodes, or returns the argument unchanged if it's not valid base-64.
* `base64encode` or `b64ecode` base-64 encodes.

Result            | Expression
------------------|-----------
`[foo]`           | `Expander.expand("[{trim}]", "    foo    "));`
`[foo]`           | `Expander.expand("[{lower}]", "FOO"));`
`[foo]`           | `Expander.expand("[{trim,lower}]", "    FOO    "));`
`[foo]`           | `Expander.expand("[{TRIMTOLOWER}]", "    FOO    "));`
`[+foo+]`         | `Expander.expand("[{urlencode}]", " foo "));`
`[ foo ]`         | `Expander.expand("[{urldecode}]", "+foo+"));`
`[foo]`           | `Expander.expand("[{urldecode}]", "foo"));`
`[Zm9vMTIzNA==]`  | `Expander.expand("[{b64encode}]", "foo1234"));`
`[foo1234]`       | `Expander.expand("[{b64decode}]", "Zm9vMTIzNA=="));`
`[cats and dogs]` | `Expander.expand("[{b64decode}]", "cats and dogs"));`


There are several substring edits available, loosely inspired by [Ruby substrings](https://ruby-doc.org/core-2.4.0/String.html#method-i-5B-5D):

* `[start]` &mdash; a substring starting at `start` (0-based inclusive) through the end of the string, equivalent to skipping the first `start` characters.
* `[start,end]` &mdash;  a substring starting at `start` and ending at end (exclusive), producing a string of length `end`-`start`.
* `[start:length]` &mdash; a substring starting at `start` with the specified `length`.
* `[/pattern/]` &mdash; the first substring matching regular expression `pattern`.
* `[/pattern/group]`&mdash; capture group `group` (can be a positional number or a named capture group) of the first substring matching `pattern`.

The `end` index may be negative, in which case the index is counted from the end of the string, where `-1` is equivalent to the length of the string (`[start]` and `[start,-1]` produce the same result), `-2` one shorter, etc.

Invalid start and end indexes and lengths are adjusted to produce a meaningful result, or the empty string if necessary (start beyond the end of the string, end before start, etc.). These scenarios typically throw exceptions in Java's `String.substring` method.

Any of `start`, `end`, `length`, `pattern`, or `group` may be taken from a parameter instead of the template by using the `{}` or `{number}` syntax.

Index-based examples:

Result         | Expression
---------------|-----------
`urge`         | `Expander.expand("{[4,8]}", "hamburger"));`
`urge`         | `Expander.expand("{[4,-2]}", "hamburger"));`
`urge`         | `Expander.expand("{[4:4]}", "hamburger"));`
`urger`        | `Expander.expand("{[4]}", "hamburger"));`
`miles`        | `Expander.expand("{[1,6]}", "smiles"));`
`miles`        | `Expander.expand("{[1,-1]}", "smiles"));`
`miles`        | `Expander.expand("{[1:5]}", "smiles"));`
`miles`        | `Expander.expand("{[1]}", "smiles"));`
`urge`         | `Expander.expand("{[{},{}]}", "hamburger", 4, 8));`
`urge`         | `Expander.expand("{[{},{}]}", "hamburger", "4", -2));`
`urge`         | `Expander.expand("{[{2}:{2}]}", "hamburger", 4));`
`urger`        | `Expander.expand("{[{}]}", "hamburger", 4.0));`
`miles`        | `Expander.expand("{[1,{}]}", "smiles", 6));`
`miles`        | `Expander.expand("{[{},-1]}", "smiles", 1));`
`miles`        | `Expander.expand("{[1:{3}]}", "smiles", 1, 5));`
`miles`        | `Expander.expand("{[{4}]}", "smiles", null, null, 1));`
`urger`        | `Expander.expand("{[4,10]}", "hamburger"));`
_empty string_ | `Expander.expand("{[4,4]}", "hamburger"));`
_empty string_ | `Expander.expand("{[4,2]}", "hamburger"));`
_empty string_ | `Expander.expand("{[4:-2]}", "hamburger"));`
_empty string_ | `Expander.expand("{[4:0]}", "hamburger"));`

Pattern-based examples:

In these examples let:

```java
String FN_EXT = "^(?<fn>.*?)(?:\\.(?<ext>[^\\.]*))?$";
```

which is a regular expression that separates a _filename_ with an _extension_, separated by `.`, into
component parts in named capture groups `fn` and `ext`.

Result         | Expression
---------------|-----------
`ell`          | `Expander.expand("{[/[aeiou](.)\\1/]}", "hello there"));`
`ell`          | `Expander.expand("{[/[aeiou](.)\\1/0]}", "hello there"));`
`l`            | `Expander.expand("{[/[aeiou](.)\\1/1]}", "hello there"));`
_empty string_ | `Expander.expand("{[/[aeiou](.)\\1/2]}", "hello there"));`
`l`            | `Expander.expand("{[/[aeiou](?<dot>.)\\1/dot]}", "hello there"));`
_empty string_ | `Expander.expand("{[/[aeiou](?<dot>.)\\1/dit]}", "hello there"));`
`foo.bar`      | `Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.bar.txt", "fn"));`
`txt`          | `Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.bar.txt", "ext"));`
`foo`          | `Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.", "fn"));`
_empty string_ | `Expander.expand("{[/"+FN_EXT+"/{}]}", "foo.", "ext"));`
`foo`          | `Expander.expand("{[/{3}/{}]}", "foo", "fn", FN_EXT));`
_empty string_ | `Expander.expand("{[/{}/{}]}", "foo", FN_EXT, "ext"));`


## Formats

The Expander supports parameters that are not Strings. By default, they will be converted to strings using the underlying Java `toString()` method, but this can be controlled using _formats_, intermingled with the edits included inside the replacement token `{}` braces.

The most general option is to use a [Java Formatter](https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html) string, which starts with `%` and ends with a single letter conversion character.

Result         | Expression
---------------|-----------
`abc   `       | `Expander.expand("{%-6s}", "abc"));`
`   abc`       | `Expander.expand("{1,%6s}", "abc"));`
` 3.142e+00`   | `Expander.expand("{%10.3e}", Math.PI));`
`3.142E+00`    | `Expander.expand("{1,%10.3e,trim,upper}", Math.PI));`

This can be used for both String (for example, for left or right justification in a fixed width) or non-String (formatting Doubles) types.

Date and time parameters can be formatted with [Java DateTimeFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) formats, optionally with a timezone adjustment (e.g. to GMT). The options are:

* `date` &mdash; formats the date with the default ISO8601 format (`yyyy-MM-dd'T'HH:mm:ss.SSSX`).
* `date()` &mdash; also uses the default format.
* `date(format)` &mdash; formats the date with the specified format.
* `date(format)[zone]` &mdash; formats the date with the specified format in the specified time zone.
* `date()[zone]` &mdash; formats the date with the default ISO8601 format in the specified time zone.

Note that to specify a `[zone]` you must supply a `(format)` &mdash; use `()` to get the default format.

Either `format` or `zone` may be taken from a parameter instead of the template by using the `{}` or `{number}` syntax.

Use `now` instead of `date` to use the current date/time instead of using a parameter.

In these examples let:

```java
Date d = new Date(1588697522346L);
```

Result                        | Expression
------------------------------|-----------
`-Tuesday(Tue)-1588697522346` | `Expander.expand("-{date(EEEE'('E'\\)')}-{%d}", d, d.getTime()));`
`2020-05-05T16:52:02.346Z`    | `Expander.expand("{date()[GMT]}", d));`

## Conditional Expansion

In some cases an expanded parameter should be surrounded with some static text, but only when the parameter has a value. For this situation, the Expander supports Conditional Expansion blocks. A Conditional Expansion block consists of sections of static text and one or more parameters: the static text is rendered only if at least one of the parameters expands to a non-empty value.

A Conditional Expansion block begins with `{?}` and ends at:

* the Conditional Expansion block terminator `{.}`,
* the next Condition Expansion block indicator `{?}`,
* the end of the string

Result         | Expression
---------------|-----------
`?a=b&c=d&e=f` | `Expander.expand("?a=b{?}&c={}{?}&e={}",              "d", "f"));`
`?a=b&e=f`     | `Expander.expand("?a=b{?}&c={}{?}&e={}",              "",  "f"));`
`?a=b`         | `Expander.expand("?a=b{?}&c={}{?}&e={[2]}the end",    "",  "f"));`
`?a=bthe end`  | `Expander.expand("?a=b{?}&c={}{?}&e={[2]}{.}the end", "",  "f"));`
