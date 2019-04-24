import java.util.*;

public class DFA implements Cloneable
{
  public int          start;
  public Set<Integer> ends = new HashSet<>();

  // The DFA's transition table is implemented with a Map, that
  // maps (State, Input) pairs to states. For example, (S1, i)
  // will be mapped to S2 if input i in sate S1 leads to state
  // S2.
  //
  private final Map<Map<Integer, Input>, Integer> transtbl;

  @Override
  public DFA clone() { return new DFA( this ); }

  public DFA()
  {
    this.transtbl = new HashMap<>();
  }

  public DFA( DFA src )
  {
    Map<Map<Integer, Input>, Integer> cloned_transtbl = new HashMap<>();
    for ( Map.Entry<Map<Integer, Input>, Integer> entry : src.transtbl.entrySet() )
    {
      cloned_transtbl.put( entry.getKey(), entry.getValue() );
    }

    this.transtbl = cloned_transtbl;
  }

  public void addTransition( int from, int to, Input in )
  {
    if ( in == Input.NONE ) return;
    if ( in == Input.EPS  ) throw new RuntimeException( "DFA does not support NULL input" );

    this.transtbl.put( new HashMap(){{ put( from, in ); }}, to );
  }

  public String simulate( Input in )
  {
    int cur_state = start;

    
  }

  public void show()
  {
    System.out.println( "DFA start state: " + this.start );
    System.out.println( "DFA fianl state(s): {" );

    if ( this.ends.size() > 0 )
      for ( int state : this.ends )
        System.out.println( "\t" + state + "," );

    System.out.println( "}" );
  }

  public static void main( String args[] )
  {
    DFA dfa = new DFA();
    dfa.addTransition( 0, 1, new Input( 'a' ) );
    dfa.addTransition( 1, 2, Input.NONE );

    try {
      dfa.addTransition( 1, 2, Input.EPS );
    } catch ( RuntimeException ex ) {
      System.out.println( "Catched the expected exception: \"" + ex + "\"" );
    }

    dfa.show();
  }
}
