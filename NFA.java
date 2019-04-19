import java.util.Vector;

public class NFA
{
  public int _start;
  public int _end;

  public final Vector<Object> _transtbl;

  public NFA( NFA src )
  {
    this._transtbl = src._transtbl;
    this._start    = src._start;
    this._end      = src._end;
  }

  public NFA( int size, int start, int end )
  {
    _transtbl = new Vector<Object>( size );

    _start = start;
    _end   = end;

    assert( isLegalState( start ) );
    assert( isLegalState( end ) );

    // Initialize _transtbl with an "empty graph",
    // no transitions between its states

    for ( int i = 0; i < size; i++ )
    {
      Vector<Input> row = new Vector<Input>();
      for ( int j = 0; j < size; j++ ) { row.add( Input.NONE ); }

      _transtbl.add( row );
    }
  }

  public int     count()               { return _transtbl.size();      }
  public boolean isLegalState( int s ) { return s >= 0 || s < count(); }

  public void addTransition( int from, int to, Input in )
  {
    assert( isLegalState( from ) );
    assert( isLegalState( to ) );

    Vector<Input> row = ( Vector<Input> )_transtbl.get( from );
    row.setElementAt( in, to );
  }

  public void show()
  {
    System.out.println( String.format( "This NFA got %d states: s0 - s%d", count(), count() - 1 ) );
    System.out.println( String.format( "The initial state is s%d", _start ) );
    System.out.println( String.format( "The final state is s%d", _end ) );

    for ( int from = 0; from < count(); from++ )
    {
      for ( int to = 0; to < count(); to++ )
      {
        Vector<Input> row = ( Vector<Input> )_transtbl.get( from );
        Input in = row.get( to );

        if ( in != Input.NONE )
        {
          System.out.print( String.format( "Transitions from s%d to s%d on input ", from, to ) );

          if   ( in == Input.EPS ) { System.out.println( "eps"); }
          else                     { System.out.println( "'" + in.v + "'" ); }
        }
      }
    }
  }

  /** Appends a new, empty state to the NFA.
   */
  public void appendEmptyState()
  {
    Vector<Input> newrow = new Vector<Input>( count() + 1 );
    for ( int i = 0; i < count(); i++ ) { newrow.add( Input.NONE ); }

    _transtbl.add( newrow );

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = ( Vector<Input> )_transtbl.get( i );
      ri.add( Input.NONE );
    }

    _end++;
  }

  /** Renames all the NFA's states:
   *
   *  For each NFA state: number += shift. Functionally, this
   *  doesn't affect the NFA, it only makes it larger and renames
   *  its states.
   */
  public void shiftStates( int shift )
  {
    if ( shift <= 0 ) { return; }

    for ( int i = 0; i < shift; i++ )
    {
      Vector<Input> newrow = new Vector<Input>( count() + shift );
      for ( int j = 0; j < count(); j++ ) { newrow.add( Input.NONE ); }

      _transtbl.insertElementAt( newrow, 0 );
    }

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = ( Vector<Input> )_transtbl.get( i );
      for ( int j = 0; j < shift; j++ ) { ri.insertElementAt( Input.NONE, 0 ); }
    }

    _start += shift;
    _end   += shift;
  }

  /** Fills states 0 up to src.count() with src's states.
   */
  public void fillStates( NFA src )
  {
    NFA dst = this;
    int srcsz = src.count();

    for ( int i = 0; i < srcsz; i++ )
    {
      for ( int j = 0; j < srcsz; j++ )
      {
        Vector<Input> rsrc = ( Vector<Input> )src._transtbl.get( i );
        Vector<Input> rdst = ( Vector<Input> )dst._transtbl.get( i );

        rdst.setElementAt( rsrc.get( j ), j );
      }
    }
  }

  public void dumpInternalTranstbl()
  {
    System.out.println( "====================" );
    System.out.println( "Initial State: " + _start );
    System.out.println( "  Final State: "   + _end );
    System.out.println( "--------------------" );
    System.out.println();

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> r = ( Vector<Input> )_transtbl.get( i );

      for ( int j = 0; j < count(); j++ )
      {
        char c;
        Input in = r.get( j );
        if      ( in == Input.NONE ) { c = '-';  }
        else if ( in == Input.EPS  ) { c = 'E';  }
        else                         { c = in.v; }

        System.out.print( String.format( " %c", c ) );
      }

      System.out.println();
    }
  }

  public NFA buildNFAAlternation( NFA nfa1, NFA nfa2 )
  {
    nfa1.shiftStates( 1 );            // make room for the new initial state
    nfa2.shiftStates( nfa1.count() ); // make room for nfa1's states

    NFA unifiedNFA = new NFA( nfa2 );
    unifiedNFA.fillStates( nfa1 );
    unifiedNFA.shiftStates( 1 );

    unifiedNFA.addTransition( 0, nfa1._start, Input.EPS );
    unifiedNFA.addTransition( 0, nfa2._start, Input.EPS );
    unifiedNFA._start = 0;

    unifiedNFA.appendEmptyState();
    unifiedNFA._end = unifiedNFA.count() - 1;

    unifiedNFA.addTransition( nfa1._end, unifiedNFA._end, Input.EPS );
    unifiedNFA.addTransition( nfa2._end, unifiedNFA._end, Input.EPS );

    return unifiedNFA;
  }

  public NFA buildNFAConcatenation( NFA nfa1, NFA nfa2 )
  {
    // Make room for nfa1's states.  First will come nfa1, then
    // nfa2 (nfa2's initial state would be overlapped with nfa1's
    // final state
    //
    nfa2.shiftStates( nfa1.count() - 1 );

    NFA unifiedNFA = new NFA( nfa2 );

    // nfa1's states take their places in unifiedNFA NOTE: nfa1's
    // final state overwrites nfa2's initial state, thus we get
    // the desired merge automagically (the transition from
    // nfa2's initial state now transits from nfa1's final state)
    // 
    unifiedNFA.fillStates( nfa1 );

    // Set the new initial state (the final state stays nfa2's
    // final state, and was already copied)
    //
    unifiedNFA._start = nfa1._start;

    return unifiedNFA;
  }

  public NFA buildNFAKleeneStar( NFA nfa )
  {
    nfa.shiftStates( 1 );

    NFA nfaKleeneStar = new NFA( nfa );
    nfaKleeneStar.fillStates( nfa );
    nfaKleeneStar.appendEmptyState();

    nfaKleeneStar._start = 0;
    nfaKleeneStar._end = nfaKleeneStar.count() - 1;

    nfaKleeneStar.addTransition( 0, nfa._start, Input.EPS );
    nfaKleeneStar.addTransition( 0, nfaKleeneStar._end, Input.EPS );
    nfaKleeneStar.addTransition( nfa._end, nfa._start, Input.EPS );
    nfaKleeneStar.addTransition( nfa._end, nfaKleeneStar._end, Input.EPS );

    return nfaKleeneStar;
  }

  public NFA buildNFABasic( Input in )
  {
    NFA nfa = new NFA( 2, 0, 1 );
    nfa.addTransition( 0, 1, in );
    return nfa;
  }

  public static void main( String args[] )
  {
    NFA nfa = new NFA( 11, 0, 10 ); 

    nfa.addTransition( 0,  1, Input.EPS ); 
    nfa.addTransition( 0,  7, Input.EPS ); 
    nfa.addTransition( 1,  2, Input.EPS ); 
    nfa.addTransition( 1,  4, Input.EPS );
    nfa.addTransition( 2,  3, new Input( 'a' ) );
    nfa.addTransition( 4,  5, new Input( 'b' ) );
    nfa.addTransition( 3,  6, Input.EPS ); 
    nfa.addTransition( 5,  6, Input.EPS ); 
    nfa.addTransition( 6,  1, Input.EPS ); 
    nfa.addTransition( 6,  7, Input.EPS ); 
    nfa.addTransition( 7,  8, new Input( 'a' ) );
    nfa.addTransition( 8,  9, new Input( 'b' ) );
    nfa.addTransition( 9, 10, new Input( 'b' ) );
   
    nfa.show();

    ///

    nfa.dumpInternalTranstbl();

    nfa.appendEmptyState(); System.out.println(); nfa.dumpInternalTranstbl();
    nfa.shiftStates( 3 );   System.out.println(); nfa.dumpInternalTranstbl();

    NFA anotherNFA = new NFA( 4, 0, 3 );

    anotherNFA.addTransition( 0, 1, new Input( 'a' ) );
    anotherNFA.addTransition( 0, 0, new Input( 'b' ) );
    anotherNFA.addTransition( 1, 2, new Input( 'b' ) );
    anotherNFA.addTransition( 2, 3, new Input( 'b' ) );
    anotherNFA.addTransition( 2, 1, new Input( 'a' ) );
    anotherNFA.addTransition( 3, 1, new Input( 'a' ) );

    System.out.println(); anotherNFA.dumpInternalTranstbl();
    nfa.fillStates( anotherNFA ); System.out.println(); nfa.dumpInternalTranstbl();
  }
}
