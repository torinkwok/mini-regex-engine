public final class Input
{
  public final Character v;

  public  Input( char v )      { this.v = v;    }
  public  Input( Character v ) { this.v = v;    }

  private Input()              { this.v = null; }

  public boolean equals( Object o )
  {
    if ( this == o ) { return true;  }
    if ( o == null ) { return false; }
    if ( getClass() != o.getClass() ) { return false; }

    Input in = ( Input )o;
    return this.v.equals( in.v );
  }

  /** Epsilon (eps) transitions are allowed in a NFA. That is,
   *  there may be a transition from state to state given "no
   *  input".
   *
   *  By definition, a Deterministic Finite Automaton is a
   *  special case of a NFA, in which:
   *
   *    1. No state has an eps-transition
   *    2. For each state S and input a, there is at most one
   *    edge labeled a leaving S.
   */
  public static Input EPS  = new Input( '\u03b5' );

  public static Input NONE = new Input();

  public static void main( String args[] )
  {
    Input in1 = Input.NONE;
    Input in2 = Input.NONE;

    Input in3 = Input.EPS;
    Input in4 = Input.EPS;

    if ( in1 == in2 ) { System.out.print( "Yes, in1 and in2 are equal\n" ); }
    if ( in3 == in4 ) { System.out.print( "Yes, in3 and in4 are equal\n" ); }

    if ( in1 != in3 ) { System.out.print( "No, in1 and in3 are not equal\n" ); }
    if ( in2 != in4 ) { System.out.print( "No, in2 and in4 are not equal\n" ); }

    Input in5 = new Input( 'c' );
    Input in6 = new Input( 'a' );
    Input in7 = new Input( 'a' );

    if ( !in5.equals( in6 ) ) { System.out.print( "No, in5 and in6 are not equal\n" ); }
    if ( in6.equals( in7 ) )  { System.out.print( "No, in6 and in7 are not equal\n" ); }
  }
}
