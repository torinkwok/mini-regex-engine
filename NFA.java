import java.util.Vector;

public class NFA
{
  public int start;
  public int end;

  public final Vector<Object> transtbl;

  public NFA( NFA src )
  {
    this.transtbl = src.transtbl;
    this.start    = src.start;
    this.end      = src.end;
  }

  public NFA( int size, int start, int end )
  {
    assert( isLegalState( start ) );
    assert( isLegalState( end ) );

    transtbl = new Vector<Object>( size );

    this.start = start;
    this.end   = end;

    // Initialize transtbl with an "empty graph",
    // no transitions between its states

    for ( int i = 0; i < size; i++ )
    {
      Vector<Input> row = new Vector<Input>();
      for ( int j = 0; j < size; j++ ) { row.add( Input.NONE ); }

      transtbl.add( row );
    }
  }

  public int     count()               { return transtbl.size();      }
  public boolean isLegalState( int s ) { return s >= 0 || s < count(); }

  public void addTransition( int from, int to, Input in )
  {
    assert( isLegalState( from ) );
    assert( isLegalState( to ) );

    Vector<Input> row = ( Vector<Input> )transtbl.get( from );
    row.setElementAt( in, to );
  }

  public void show()
  {
    System.out.println( String.format( "This NFA got %d states: s0 - s%d", count(), count() - 1 ) );
    System.out.println( String.format( "The initial state is s%d", start ) );
    System.out.println( String.format( "The final state is s%d", end ) );

    for ( int from = 0; from < count(); from++ )
    {
      for ( int to = 0; to < count(); to++ )
      {
        Vector<Input> row = ( Vector<Input> )transtbl.get( from );
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

    transtbl.add( newrow );

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = ( Vector<Input> )transtbl.get( i );
      ri.add( Input.NONE );
    }

    end++;
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

      transtbl.insertElementAt( newrow, 0 );
    }

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> ri = ( Vector<Input> )transtbl.get( i );
      for ( int j = 0; j < shift; j++ ) { ri.insertElementAt( Input.NONE, 0 ); }
    }

    start += shift;
    end   += shift;
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
        Vector<Input> rsrc = ( Vector<Input> )src.transtbl.get( i );
        Vector<Input> rdst = ( Vector<Input> )dst.transtbl.get( i );

        rdst.setElementAt( rsrc.get( j ), j );
      }
    }
  }

  public void dumpInternalTranstbl()
  {
    System.out.println( "====================" );
    System.out.println( "Initial State: " + start );
    System.out.println( "  Final State: " + end );
    System.out.println( "--------------------" );
    System.out.println();

    for ( int i = 0; i < count(); i++ )
    {
      Vector<Input> r = ( Vector<Input> )transtbl.get( i );

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

    NFA nfaUnion = new NFA( nfa2 );
    nfaUnion.fillStates( nfa1 );
    nfaUnion.shiftStates( 1 );

    nfaUnion.addTransition( 0, nfa1.start, Input.EPS );
    nfaUnion.addTransition( 0, nfa2.start, Input.EPS );
    nfaUnion.start = 0;

    nfaUnion.appendEmptyState();
    nfaUnion.end = nfaUnion.count() - 1;

    nfaUnion.addTransition( nfa1.end, nfaUnion.end, Input.EPS );
    nfaUnion.addTransition( nfa2.end, nfaUnion.end, Input.EPS );

    return nfaUnion;
  }

  public NFA buildNFAConcatenation( NFA nfa1, NFA nfa2 )
  {
    // Make room for nfa1's states.  First will come nfa1, then
    // nfa2 (nfa2's initial state would be overlapped with nfa1's
    // final state
    //
    nfa2.shiftStates( nfa1.count() - 1 );

    NFA nfaConcat = new NFA( nfa2 );

    // nfa1's states take their places in nfaConcat NOTE: nfa1's
    // final state overwrites nfa2's initial state, thus we get
    // the desired merge automagically (the transition from
    // nfa2's initial state now transits from nfa1's final state)
    // 
    nfaConcat.fillStates( nfa1 );

    // Set the new initial state (the final state stays nfa2's
    // final state, and was already copied)
    //
    nfaConcat.start = nfa1.start;

    return nfaConcat;
  }

  public NFA buildNFAKleeneStar( NFA nfa )
  {
    nfa.shiftStates( 1 );

    NFA nfaKleeneStar = new NFA( nfa );
    nfaKleeneStar.fillStates( nfa );
    nfaKleeneStar.appendEmptyState();

    nfaKleeneStar.start = 0;
    nfaKleeneStar.end = nfaKleeneStar.count() - 1;

    nfaKleeneStar.addTransition( 0, nfa.start, Input.EPS );
    nfaKleeneStar.addTransition( 0, nfaKleeneStar.end, Input.EPS );
    nfaKleeneStar.addTransition( nfa.end, nfa.start, Input.EPS );
    nfaKleeneStar.addTransition( nfa.end, nfaKleeneStar.end, Input.EPS );

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
