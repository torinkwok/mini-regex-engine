A real Regular Expression engine. It's not a complete engine like the one Lex or Perl have, but it's a start.

People familiar with regexes know that there are more complicated forms than * and |. However, anything can be built from `*`, `|` and `eps`. For instance, `x?` (zero or one instance of x) is a shorthand for `(x|eps)`. `x+` (one or more instances of x) is a shorthand for `xx*`. The basis has been laid, the rest is just extensions.
