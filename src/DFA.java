import java.util.*;

public class DFA implements Cloneable
{
  public State      start = State.ZERO;
  public Set<State> ends = new HashSet<>();

  /** The DFA's transition table is implemented with a Map, that
   *  maps (State, Input) pairs to states. For example, (S1, i)
   *  will be mapped to S2 if input i in sate S1 leads to state
   *  S2.
   */
  public final Map<Map<State, Input>, State> transtbl;

  @Override
  public DFA clone() { return new DFA( this ); }

  public DFA() { this.transtbl = new HashMap<>(); }

  public DFA( DFA src )
  {
    Map<Map<State, Input>, State> cloned_transtbl = new HashMap<>();

    for ( Map.Entry<Map<State, Input>, State> entry : src.transtbl.entrySet() )
      cloned_transtbl.put( entry.getKey(), entry.getValue() );

    this.transtbl = cloned_transtbl;
  }

  public void addTransition( int   from, int   to, Input in ) { addTransition( new State( from ), new State( to ), in ); }
  public void addTransition( int   from, State to, Input in ) { addTransition( new State( from ), to,              in ); }
  public void addTransition( State from, int   to, Input in ) { addTransition( from,              new State( to ), in ); }
  public void addTransition( State from, State to, Input in )
  {
    if ( in == Input.NONE ) return;
    if ( in == Input.EPS  ) throw new RuntimeException( "DFA does not support NULL input" );

    this.transtbl.put( new HashMap(){{ put( from, in ); }}, to );
  }

  public  boolean simulate( String to_recog ) { return _simulate( this.start, to_recog ); }
  private boolean _simulate( State currentState, String in )
  {
    if ( in.length() == 0 )
      return this.ends.contains( currentState );

    Input c = new Input( in.charAt( 0 ) );

    Map<State, Input> from_pair = new HashMap(){{ put( currentState, c ); }};
    State to_state = this.transtbl.get( from_pair );

    if ( to_state != null )
      return _simulate( to_state, in.substring( 1 ) );

    return false;
  }

  public void show()
  {
    System.out.println( "DFA start state: " + this.start );
    System.out.println( "DFA final state(s): {" );

    if ( this.ends.size() > 0 )
      for ( State sf : this.ends )
        System.out.println( "\t" + sf + "," );

    System.out.println( "}" );
  }

  public static void main( String args[] )
  {
    DFA dfa = new DFA();

    dfa.addTransition( 0, 1, new Input( 'a' ) );
    dfa.addTransition( 1, 1, new Input( 'a' ) );
    dfa.addTransition( 1, 2, Input.NONE );
    dfa.addTransition( 1, 2, new Input( 's' ) );
    dfa.addTransition( 0, 0, new Input( 'b' ) );

    dfa.ends.add( State.ZERO );
    dfa.ends.add( new State( 1 ) );
    dfa.ends.add( new State( 2 ) );

    try {
      dfa.addTransition( 1, 2, Input.EPS );
    } catch ( RuntimeException ex ) {
      System.out.println( "Catched the expected exception: \"" + ex + "\"" );
    }

    dfa.show();

    assert dfa.simulate( "aaaas" );
    assert dfa.simulate( "bbbb" );
    assert !dfa.simulate( "st" );
  }
}
